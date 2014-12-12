package nl.jeltef.myomote;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;

import java.text.DecimalFormat;

public class MyoService extends Service {
    public MyoService() {
    }
    private final IBinder mBinder = new LocalBinder();
    private static final String TAG = "MyoService";
    private VlcService mVlcService;
    public boolean mVlcBound;
    private SetupActivity.MyoSetupFragment mFragment = null;
    public Hub mHub;


    private Arm mArm = Arm.UNKNOWN;
    private XDirection mXDirection = XDirection.UNKNOWN;

    private int mRollOffset = 0;
    private int mRoll;
    private int mPrevRoll = 0;
    private int mPitch;
    private int mYaw;

    private boolean mActive = false;
    private Pose mCurPose = Pose.UNKNOWN;
    private boolean mGestureActionDone = false;
    private long mTimeOfLastAction = 0;
    private long timeBetweenActions = 700000000L;
    private int mColor = Color.BLACK;
    private String mPoseTextValue = "Pose";

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        MyoService getService() {
            return MyoService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating hub");
        mHub = Hub.getInstance();
        if (!mHub.init(this, getPackageName())) {
            Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }
        Intent intent = new Intent(this, VlcService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        mHub.attachToAdjacentMyo();
    }

    public void setFragment(SetupActivity.MyoSetupFragment fragment) {
        mFragment = fragment;
        mFragment.mPoseText.setTextColor(mColor);
        mFragment.mPoseText.setText(mPoseTextValue);
    }

    public void unsetFragment() {
        mFragment = null;
    }

    private void setPoseText(String text) {
        mPoseTextValue = text;

        if (mFragment != null) {
            mFragment.mPoseText.setText(text);
        }
    }

    public DeviceListener mListener = new AbstractDeviceListener() {



        public void onConnect(Myo myo, long timestamp) {
            setPoseText("Myo Connected");
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            setPoseText("Myo Disconnected!");

        }

        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            setPoseText("Pose: " + pose);


            if (mActive && pose == Pose.FINGERS_SPREAD) {

            }


            mCurPose = pose;
            mGestureActionDone = false;
        }

        // onArmRecognized() is called whenever Myo has recognized a setup gesture after someone has put it on their
        // arm. This lets Myo know which arm it's on and which way it's facing.
        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            mArm = arm;
            mXDirection = xDirection;
            mRollOffset = mRoll - 9; //Save initial mRoll plus tiny offset for turning arm
        }

        // onArmLost() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
        // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
        // when Myo is moved around on the arm.
        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            mArm = Arm.UNKNOWN;
            mXDirection = XDirection.UNKNOWN;
            mRollOffset = 0;
        }

        @Override
        public void onAccelerometerData(Myo myo, long timestamp, Vector3 vec) {
            if (mFragment != null) {
                mFragment.mAccelText.setText("Acc: " + vec.length());
            }
            if (mCurPose == Pose.THUMB_TO_PINKY && !mGestureActionDone && (vec.length() > 2 || mActive)) {
                if (!mActive) {
                    myo.vibrate(Myo.VibrationType.SHORT);
                    mColor = Color.GREEN;

                }
                else {
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    mColor = Color.BLACK;

                }

                if (mFragment != null) {
                    mFragment.mPoseText.setTextColor(mColor);
                }

                mGestureActionDone = true;
                mActive = !mActive;
            }
        }


        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            DecimalFormat twoDForm = new DecimalFormat("#.##");

            mPrevRoll = mRoll;
            mRoll = (int) Math.toDegrees(Quaternion.roll(rotation)) - mRollOffset;
            mPitch = (int) (Math.toDegrees(Quaternion.pitch(rotation)));
            mYaw = (int) Math.toDegrees(Quaternion.yaw(rotation));
            if (mFragment != null) {
                mFragment.mOrientText.setText(
                        "Pitch: " + mPitch +
                                "\nRoll: " + mRoll +
                                "\nYaw: " + mYaw
                );
            }

            // Adjust mRoll and pitch for the orientation of the Myo on the arm.
            if (mXDirection == XDirection.TOWARD_ELBOW) {
                mPitch *= -1;
                mYaw += 180;
                mRoll *= -1;
            }

            if (mActive) {
                switch (mCurPose) {
                    case FIST:
                        mVlcService.changeVolume(rollDifference());
                        actionDone();
                        break;
                    case WAVE_IN:
                        if (canDoNewAction()) {
                            mVlcService.rewind();
                            actionDone();
                        }
                    case WAVE_OUT:
                        if (canDoNewAction()) {
                            mVlcService.fastForward();
                            actionDone();
                        }
                        break;
                    case FINGERS_SPREAD:
                        if (canDoNewAction() && !mGestureActionDone) {
                            mVlcService.togglePlay();
                            actionDone();
                        }
                        break;
                }
            }
        }


        private void actionDone() {
            mGestureActionDone = true;
            mTimeOfLastAction = System.nanoTime();
        }

        private boolean canDoNewAction() {
            return mTimeOfLastAction + timeBetweenActions < System.nanoTime();
        }

        private int rollDifference() {
            if (mRoll > mPrevRoll && mRoll - mPrevRoll < 100 ||
                    mRoll < mPrevRoll && mPrevRoll - mRoll < 100) {
                return mRoll - mPrevRoll;
            }
            return 0;
        }
    };


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder b) {
            // We've bound to VlcService, cast the IBinder and get VlcService instance
            VlcService.LocalBinder binder = (VlcService.LocalBinder) b;
            Log.d(TAG, "Connected to VLC service");
            mVlcService = binder.getService();
            mHub.addListener(mListener);

            mVlcBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mVlcBound = false;
        }
    };
}

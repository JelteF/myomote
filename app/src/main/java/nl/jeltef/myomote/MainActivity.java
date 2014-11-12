package nl.jeltef.myomote;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import java.text.DecimalFormat;

public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private final static String TAG = "MainActivity";
    private DeviceListener mListener;
    private SeekBar mSeekBar;
    private TextView percentText;
    private TextView poseText;
    private TextView orientText;



    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private Hub hub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        percentText = (TextView) findViewById(R.id.percent_text);
        poseText = (TextView) findViewById(R.id.current_pose);
        orientText = (TextView) findViewById(R.id.orientation_text);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                percentText.setText("" + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        hub = Hub.getInstance();
        if (!hub.init(this)) {
            Log.e(TAG, "Could not initialize hub");
            finish();
            return;
        }
        createMyoListener();

        Hub.getInstance().addListener(mListener);
        Hub.getInstance().pairWithAnyMyo();

    }

    private void createMyoListener() {
        mListener = new AbstractDeviceListener() {
            private Arm mArm = Arm.UNKNOWN;
            private XDirection mXDirection = XDirection.UNKNOWN;

            private int rollOffset = 0;
            private int roll;
            private int prevRoll = 0;
            private int pitch;
            private int yaw;

            private boolean active = false;
            private Pose curPose = Pose.UNKNOWN;
            private boolean powerLocked = false;
            private int powerLockingStage = 0;
            private long powerLockingStart = 0;



            public void onConnect(Myo myo, long timestamp) {
                poseText.setText("Myo Connected!");
            }

            @Override
            public void onDisconnect(Myo myo, long timestamp) {
                poseText.setText("Myo Disconnected!");
            }

            @Override
            public void onPose(Myo myo, long timestamp, Pose pose) {
                poseText.setText("Pose: " + pose);
                if (powerLockingStage == 3 && pose != Pose.FINGERS_SPREAD) {
                    powerLockingStage = 0;
                }

                if (!powerLocked && powerLockingStage == 0 && pose == Pose.THUMB_TO_PINKY) {
                    if (!active) {
                        myo.vibrate(Myo.VibrationType.SHORT);
                    }
                    else {
                        myo.vibrate(Myo.VibrationType.MEDIUM);
                    }
                    active = !active;
                }

                curPose = pose;
            }

            // onArmRecognized() is called whenever Myo has recognized a setup gesture after someone has put it on their
            // arm. This lets Myo know which arm it's on and which way it's facing.
            @Override
            public void onArmRecognized(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
                mArm = arm;
                mXDirection = xDirection;
                rollOffset = roll - 9; //Save initial roll plus tiny offset for turning arm
            }
            // onArmLost() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
            // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
            // when Myo is moved around on the arm.
            @Override
            public void onArmLost(Myo myo, long timestamp) {
                mArm = Arm.UNKNOWN;
                mXDirection = XDirection.UNKNOWN;
                rollOffset = 0;
            }


            @Override
            public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
                DecimalFormat twoDForm = new DecimalFormat("#.##");

                prevRoll = roll;
                roll = (int) Math.toDegrees(Quaternion.roll(rotation)) - rollOffset;
                pitch = (int) (Math.toDegrees(Quaternion.pitch(rotation)));
                yaw = (int) Math.toDegrees(Quaternion.yaw(rotation));
                orientText.setText(
                        "Pitch: " + pitch +
                        "\nRoll: " + roll +
                        "\nYaw: " + yaw
                );

                // Adjust roll and pitch for the orientation of the Myo on the arm.
                if (mXDirection == XDirection.TOWARD_ELBOW) {
                    roll *= -1;
                    pitch *= -1;
                }

                orientText.setRotation(roll);
                orientText.setRotationX(pitch);
                orientText.setRotationY(yaw);

                // Power(un)locking should take no more than 4 seconds
                if (powerLockingStage != 0 && powerLockingStage != 3 &&
                        System.nanoTime() > powerLockingStart + 4000000000L) {
                    powerLockingStage = 0;
                    if (!powerLocked) {
                        poseText.setTextColor(Color.BLACK);
                    }
                    else {
                        poseText.setTextColor(Color.RED);
                    }

                }

                if (active && powerLockingStage == 0 &&
                        (curPose == Pose.FINGERS_SPREAD || curPose == Pose.FIST)) {
                    mSeekBar.setProgress(mSeekBar.getProgress() + roll - prevRoll);
                }

                // Power(un)locking works in three stages, hold thumb to pinky when moving your
                // arm from bottom to top and then spread your fingers.
                if (curPose == Pose.THUMB_TO_PINKY) {
                    if (pitch > 50) {
                        powerLockingStage = 1;
                        powerLockingStart = System.nanoTime();
                        poseText.setTextColor(Color.BLUE);

                    }
                    else if (powerLockingStage == 1 && pitch < -50) {
                        powerLockingStage = 2;
                        poseText.setTextColor(Color.YELLOW);

                    }
                }
                else if (powerLockingStage == 2 && curPose == Pose.FINGERS_SPREAD) {
                    powerLockingStage = 3;
                    powerLocked = !powerLocked;
                    active = !powerLocked;
                    if (powerLocked) {
                        poseText.setTextColor(Color.RED);
                        myo.vibrate(Myo.VibrationType.LONG);

                    }
                    else {
                        poseText.setTextColor(Color.BLACK);
                        myo.vibrate(Myo.VibrationType.SHORT);
                        myo.vibrate(Myo.VibrationType.SHORT);
                    }
                }
            }
        };
    }

    public void pairMyo(View view) {
        Log.e(TAG, "Searching for Myo");
        Intent intent = new Intent(this, ScanActivity.class);
        this.startActivity(intent);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}

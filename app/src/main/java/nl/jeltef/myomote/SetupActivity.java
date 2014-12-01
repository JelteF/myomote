package nl.jeltef.myomote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;


import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.peterbaldwin.vlcremote.sweep.PortSweeper;


public class SetupActivity extends Activity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    private final static String TAG = "SetupActivity";
    private static DeviceListener mListener;
    private static SeekBar mSeekBar;
    private static TextView percentText;
    private static TextView poseText;
    private static TextView orientText;
    private static TextView accelText;
    private VlcFragment mVlcFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setup, menu);
        return true;
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

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a MyoSetupFragment (defined as a static inner class below).
            if (position == 0) {
                return MyoSetupFragment.newInstance();
            }
            else {
                mVlcFragment = VlcFragment.newInstance();
                return mVlcFragment;
            }
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class MyoSetupFragment extends Fragment {
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static MyoSetupFragment newInstance() {
            MyoSetupFragment fragment = new MyoSetupFragment();
            Bundle args = new Bundle();
            fragment.setArguments(args);
            return fragment;
        }

        public MyoSetupFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_myo_setup, container, false);

            mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
            percentText = (TextView) view.findViewById(R.id.percent_text);
            poseText = (TextView) view.findViewById(R.id.current_pose);
            orientText = (TextView) view.findViewById(R.id.orientation_text);
            accelText = (TextView) view.findViewById(R.id.accelerometer_text);

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


            Hub hub = Hub.getInstance();
            SetupActivity activity = (SetupActivity) this.getActivity();
            if (!hub.init(activity)) {
                Log.e(TAG, "Could not initialize hub");
                activity.finish();
                return view;
            }
            activity.createMyoListener();

            Hub.getInstance().addListener(mListener);
            Hub.getInstance().attachToAdjacentMyo();


            return view;



        }

        public void pairMyo(View view) {
            Log.e(TAG, "Searching for Myo");
            Intent intent = new Intent(this.getActivity(), ScanActivity.class);
            this.startActivity(intent);
        }
    }
    /**
     * Begin GPLv3 code
     */
    private WifiInfo getConnectionInfo() {
        Object service = getSystemService(WIFI_SERVICE);
        WifiManager manager = (WifiManager) service;
        WifiInfo info = manager.getConnectionInfo();
        if (info != null) {
            SupplicantState state = info.getSupplicantState();
            if (state.equals(SupplicantState.COMPLETED)) {
                return info;
            }
        }
        return null;
    }
    /**
     * End GPLv3 code
     */

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class VlcFragment extends Fragment implements PortSweeper.Callback {
        private String mHostName;
        private String mPassword;
        private String mBaseUrl;
        private String mSeekTime = "10";

        private EditText mPasswordField;
        private TextView mIpAddressText;
        private SeekBar mTimeSeekBar;
        private TextView mTimeText;
        private SeekBar mVolumeSeekBar;

        private boolean mConnected = false;
        private boolean mChangingTime = false;
        private boolean mChangingVolume = false;
        long mLastRequest = 0;
        int mVolumeDifferenceTotal = 0;



        private final static int UPDATE_INTERVAL = 500; //half a second
        private final static long REQUEST_DISTANCE = 60000000;

        Runnable mUpdateTask;
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static VlcFragment newInstance() {
            VlcFragment fragment = new VlcFragment();
            Bundle args = new Bundle();
            fragment.setArguments(args);
            return fragment;
        }

        public VlcFragment() {
        }

        public SetupActivity getSetupActivity() {
            return (SetupActivity) getActivity();
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View view = inflater.inflate(R.layout.fragment_vlc_setup, container, false);

            mPort = 8080;
            mPath = "/requests/";
            mWorkers = DEFAULT_WORKERS;

            mPortSweeper = createPortSweeper(this);
            mPasswordField = (EditText) view.findViewById(R.id.passwordField);
            mIpAddressText = (TextView) view.findViewById(R.id.ipAddressText);
            mTimeSeekBar = (SeekBar) view.findViewById(R.id.timeSeekBar);
            mTimeText = (TextView) view.findViewById(R.id.timeText);
            mVolumeSeekBar = (SeekBar) view.findViewById(R.id.volumeSeekBar);

            mTimeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int full_seconds, boolean fromUser) {
                    long hours = TimeUnit.SECONDS.toHours(full_seconds);
                    String hour_string;
                    long minutes = TimeUnit.SECONDS.toMinutes(full_seconds) -
                            TimeUnit.HOURS.toMinutes(hours);
                    long seconds = full_seconds - TimeUnit.MINUTES.toSeconds(minutes) -
                            TimeUnit.HOURS.toSeconds(hours);

                    if (hours > 0) {
                         hour_string = String.format("%02d:", hours);
                    }
                    else
                        hour_string = "";

                    mTimeText.setText(hour_string + String.format("%02d:%02d", minutes, seconds));

                    if (fromUser) {
                        // Don't overload VLC with requests.
                        if (requestAllowed()) {
                            sendStatusCommand("seek", "val=" + full_seconds);
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mChangingTime = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mChangingTime = false;
                }
            });

            mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                    if (fromUser) {
                        // Don't overload VLC with requests.
                        if (requestAllowed()) {
                            sendStatusCommand("volume", "val=" + i);
                        }

                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mChangingVolume = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mChangingVolume = false;
                }
            });

            mUpdateTask = new Runnable()
            {
                @Override
                public void run() {
                    sendStatusCommand("", "");
                    getView().postDelayed(mUpdateTask, UPDATE_INTERVAL);
                }
            };

            return view;
        }

        private boolean requestAllowed() {
            return (mLastRequest + REQUEST_DISTANCE) < System.nanoTime();
        }

        @Override
        public void onHostFound(String hostname, int responseCode) {
            mHostName = hostname;
            mIpAddressText.setText("Found at " + hostname + ":8080");
        }

        @Override
        public void onProgress(int progress, int max) {
            if (progress == max) {
                Log.d(TAG, "Finished sweeping");

            }
        }

        public void connect() {
            mPassword = Uri.encode(mPasswordField.getText().toString());
            mBaseUrl = "http://" + mHostName + ":" + mPort + mPath;
            //mPasswordField.setText(mBaseUrl);
            startCheckingStatus();
            mConnected = true;
        }

        public void togglePlay() {
            sendStatusCommand("pl_pause", "");
        }

        public void fastForward() {
            sendStatusCommand("seek", "val=+" + mSeekTime);
        }

        public void rewind() {
            sendStatusCommand("seek", "val=-" + mSeekTime);
        }

        public void next() {
            sendStatusCommand("pl_next", "val=-" + mSeekTime);
        }

        public void changeVolume(int difference) {
            String param;
            mVolumeDifferenceTotal += difference;

            if (!requestAllowed()) {
                return;
            }

            mVolumeDifferenceTotal *= 5;
            if (mVolumeDifferenceTotal >= 0) {
                param = "+" + mVolumeDifferenceTotal;
            }
            else {
                param = "" + mVolumeDifferenceTotal;
            }

            sendStatusCommand("volume", "val=" + param);
            mVolumeDifferenceTotal = 0;
        }

        private void previous() {
            sendStatusCommand("pl_previous", "val=-" + mSeekTime);
        }

        private void sendStatusCommand(String command, String params) {
            HttpGet request = new HttpGet(mBaseUrl + "status.json?command=" + command +
                    "&" + params);
            request.addHeader(BasicScheme.authenticate(
                    new UsernamePasswordCredentials("", mPassword), "UTF-8", false));
            if (mConnected) {
                new SendRequest().execute(request);
            }

        }

        public void click(View view) {
            switch(view.getId()) {
                case R.id.playButton:
                    togglePlay();
                    break;
                case R.id.forwardButton:
                    fastForward();
                    break;
                case R.id.rewindButton:
                    rewind();
                    break;
                case R.id.nextButton:
                    next();
                    break;
                case R.id.previousButton:
                    previous();
                    break;
                default:
                    Log.d(TAG, "Something unknown was clicked, it had id: " + view.getId());
                    break;
            }
        }

        void startCheckingStatus()
        {
            mUpdateTask.run();
        }

        void stopCheckingStatus()
        {
            getView().removeCallbacks(mUpdateTask);
        }

        class SendRequest extends AsyncTask<HttpGet, Void, JSONObject> {

            protected JSONObject doInBackground(HttpGet... request) {

                mLastRequest = System.nanoTime();

                HttpClient client = new DefaultHttpClient();
                //Log.d(TAG, "Sending request to: " + request[0].getURI());

                try {
                    HttpResponse response = client.execute(request[0]);
                    //Log.d(TAG, "Request status code: " + response.getStatusLine().getStatusCode());

                    // Source: http://stackoverflow.com/a/2845612/2570866
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    for (String line = null; (line = reader.readLine()) != null;) {
                        builder.append(line).append("\n");
                    }

                    JSONTokener tokener = new JSONTokener(builder.toString());
                    JSONObject result = new JSONObject(tokener);

                    return result;
                } catch (IOException e) {
                    stopCheckingStatus();
                    e.printStackTrace();
                    return null;
                } catch (JSONException e) {
                    stopCheckingStatus();
                    e.printStackTrace();
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                if (result == null) {
                    return;
                }
                try {
                    if (!mChangingVolume)
                        mVolumeSeekBar.setProgress(result.getInt("volume"));
                    if (!mChangingTime) {
                        mTimeSeekBar.setMax(result.getInt("length"));
                        mTimeSeekBar.setProgress(result.getInt("time"));
                    }

                } catch (JSONException e) {
                    stopCheckingStatus();
                    e.printStackTrace();
                }
            }
        }

        /**
         * Start GPLv3 code
         */
        private PortSweeper mPortSweeper;
        private String mPath;
        private int mPort;
        private int mWorkers;
        public static final int DEFAULT_WORKERS = 16;

        private static byte[] toByteArray(int i) {
            int i4 = (i >> 24) & 0xFF;
            int i3 = (i >> 16) & 0xFF;
            int i2 = (i >> 8) & 0xFF;
            int i1 = i & 0xFF;
            return new byte[] {
                    (byte) i1, (byte) i2, (byte) i3, (byte) i4
            };
        }


        private PortSweeper createPortSweeper(PortSweeper.Callback callback) {
            return new PortSweeper(mPort, mPath, mWorkers, callback, Looper.myLooper());
        }

        private byte[] getIpAddress() {
            WifiInfo info = getSetupActivity().getConnectionInfo();
            if (info != null) {
                return toByteArray(info.getIpAddress());
            }
            return null;
        }

        public void startSweep() {
            byte[] ipAddress = getIpAddress();
            if (ipAddress != null) {
                mPortSweeper.sweep(ipAddress);
            }
        }
        /**
         * End GPLv3 code
         */
    }


    private void createMyoListener() {
        mListener = new AbstractDeviceListener() {
            private Arm mArm = Arm.UNKNOWN;
            private XDirection mXDirection = XDirection.UNKNOWN;

            private int mRollOffset = 0;
            private int mRoll;
            private int mPrevRoll = 0;
            private int mPitch;
            private int mYaw;

            private boolean mActive = false;
            private long mActiveSince = 0;
            private Pose mCurPose = Pose.UNKNOWN;
            private boolean mGestureActionDone = false;
            private long mTimeOfLastAction = 0;


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

                if (mActive && pose == Pose.FINGERS_SPREAD) {
                    mVlcFragment.togglePlay();
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
                accelText.setText("Acc: " + vec.length());
                if (mCurPose == Pose.THUMB_TO_PINKY && !mGestureActionDone && (vec.length() > 2 || mActive)) {
                    if (!mActive) {
                        myo.vibrate(Myo.VibrationType.SHORT);
                        poseText.setTextColor(Color.GREEN);
                    }
                    else {
                        myo.vibrate(Myo.VibrationType.MEDIUM);
                        poseText.setTextColor(Color.BLACK);

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
                orientText.setText(
                        "Pitch: " + mPitch +
                                "\nRoll: " + mRoll +
                                "\nYaw: " + mYaw
                );

                // Adjust mRoll and pitch for the orientation of the Myo on the arm.
                if (mXDirection == XDirection.TOWARD_ELBOW) {
                    mPitch *= -1;
                    mYaw += 180;
                    mRoll *= -1;
                }

                orientText.setRotation(mRoll);
                orientText.setRotationX(mPitch);
                orientText.setRotationY(mYaw);



                if (mActive && mCurPose == Pose.FIST &&
                        (mRoll > mPrevRoll && mRoll - mPrevRoll < 100 ||
                        mRoll < mPrevRoll && mPrevRoll - mRoll < 100)) {
                    mVlcFragment.changeVolume(mRoll - mPrevRoll);
                }
            }
        };
    }

    public void pairMyo(View view) {
        Log.d(TAG, "Searching for Myo");
        Intent intent = new Intent(this, ScanActivity.class);
        this.startActivity(intent);
    }

    public void findVLC(View view) {
        Log.d(TAG, "Searching for VLC");
        mVlcFragment.startSweep();
    }

    public void connectVLC(View view) {
        mVlcFragment.connect();
    }

    public void clickVlcFragment(View view) {
        mVlcFragment.click(view);
    }

}

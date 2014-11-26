package nl.jeltef.myomote;

import java.text.DecimalFormat;
import java.util.Locale;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
        /*

        */

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
            else
                return VlcSetupFragment.newInstance();
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
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
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
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
     * A placeholder fragment containing a simple view.
     */
    public static class VlcSetupFragment extends Fragment {
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static VlcSetupFragment newInstance() {
            VlcSetupFragment fragment = new VlcSetupFragment();
            Bundle args = new Bundle();
            fragment.setArguments(args);
            return fragment;
        }

        public VlcSetupFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_vlc_setup, container, false);


            return view;
        }
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
            public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
                mArm = arm;
                mXDirection = xDirection;
                rollOffset = roll - 9; //Save initial roll plus tiny offset for turning arm
            }
            // onArmLost() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
            // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
            // when Myo is moved around on the arm.
            @Override
            public void onArmUnsync(Myo myo, long timestamp) {
                mArm = Arm.UNKNOWN;
                mXDirection = XDirection.UNKNOWN;
                rollOffset = 0;
            }

            @Override
            public void onAccelerometerData(Myo myo, long timestamp, Vector3 vec) {
                accelText.setText("Acc: " + vec.length());
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
                    pitch *= -1;
                    yaw += 180;
                    roll *= -1;
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

}

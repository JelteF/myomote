package nl.jeltef.myomote;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
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

import org.json.JSONException;
import org.json.JSONObject;


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
        //getMenuInflater().inflate(R.menu.menu_setup, menu);
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
                return VlcFragment.newInstance();
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

    private String getFragmentTag(int fragmentPosition) {
        return "android:switcher:" + R.id.pager + ":" + fragmentPosition;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class MyoSetupFragment extends Fragment {
        public TextView mPoseText;
        public TextView mOrientText;
        public TextView mAccelText;

        private static final String TAG = "MyoFragment";


        public MyoService mService;
        private boolean mBound = false;
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

            mPoseText = (TextView) view.findViewById(R.id.current_pose);
            mOrientText = (TextView) view.findViewById(R.id.orientation_text);
            mAccelText = (TextView) view.findViewById(R.id.accelerometer_text);

            return view;
        }

        @Override
        public void onStart() {
            Log.d(TAG, "starting fragment");

            super.onStart();
            Intent intent = new Intent(this.getActivity(), MyoService.class);
            this.getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        @Override
        public void onStop() {
            Log.d(TAG, "stopping fragment");
            super.onStop();
            if (mBound) {
                mService.unsetFragment();
                this.getActivity().unbindService(mConnection);
                mBound = false;
            }
        }

        /** Defines callbacks for service binding, passed to bindService() */
        private ServiceConnection mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder b) {
                // We've bound to VlcService, cast the IBinder and get VlcService instance
                MyoService.LocalBinder binder = (MyoService.LocalBinder) b;
                Log.d(TAG, "Connected to VLC service");
                mService = binder.getService();
                mService.setFragment(MyoSetupFragment.this);

                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };

    }


    public static class VlcFragment extends Fragment implements VlcService.Callback {
        private EditText mPasswordField;
        private TextView mIpAddressText;
        private SeekBar mTimeSeekBar;
        private TextView mTimeText;
        private SeekBar mVolumeSeekBar;

        private String mHostname;
        private boolean mChangingTime = false;
        private boolean mChangingVolume = false;
        long mLastRequest = 0;
        public VlcService mService;
        private boolean mBound = false;

        private static final String TAG = "VlcFragment";




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
                        if (mService.requestAllowed()) {
                            mService.sendStatusCommand("seek", "val=" + full_seconds);
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
                        if (mService.requestAllowed()) {
                            mService.sendStatusCommand("volume", "val=" + i);
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

            mUpdateTask = new Runnable() {
                @Override
                public void run() {
                    mService.sendStatusCommand("", "");
                    try {
                        getView().postDelayed(mUpdateTask, UPDATE_INTERVAL);
                    }
                    catch (NullPointerException e) {
                        e.getStackTrace();
                    }
                }
            };

            return view;
        }

        @Override
        public void onStart() {
            Log.d(TAG, "starting fragment");

            super.onStart();
            Intent intent = new Intent(this.getActivity(), VlcService.class);
            this.getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        @Override
        public void onStop() {
            Log.d(TAG, "stopping fragment");
            super.onStop();
            if (mBound) {
                mService.mCallback = null;
                this.getActivity().unbindService(mConnection);
                mBound = false;
            }
        }

        public void connect() {
            mService.connect(mPasswordField.getText().toString());
        }

        public void click(View view) {
            if (!mBound) {
                return;
            }

            switch(view.getId()) {
                case R.id.playButton:
                    mService.togglePlay();
                    break;
                case R.id.forwardButton:
                    mService.fastForward();
                    break;
                case R.id.rewindButton:
                    mService.rewind();
                    break;
                case R.id.nextButton:
                    mService.next();
                    break;
                case R.id.previousButton:
                    mService.previous();
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

        public void onHostFound(String hostname) {
            mHostname = hostname;
            mIpAddressText.setText("Found at " + mHostname);
        }

        public void onConnected() {
            mIpAddressText.setText("Connected to " + mHostname);
            startCheckingStatus();
        }

        public void onDisconnected() {
            mIpAddressText.setText("Disconnected");
            stopCheckingStatus();
        }

        public void onUpdate(JSONObject result) {
            if (result == null) {
                stopCheckingStatus();
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

        /** Defines callbacks for service binding, passed to bindService() */
        private ServiceConnection mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder b) {
                // We've bound to VlcService, cast the IBinder and get VlcService instance
                VlcService.LocalBinder binder = (VlcService.LocalBinder) b;
                Log.d(TAG, "Connected to VLC service");
                mService = binder.getService();
                if (mService.addCallback(VlcFragment.this)) {
                    mHostname = mService.mHostName;
                    mPasswordField.setText(mService.mPassword);
                    onConnected();
                }

                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
                mService.mCallback = null;
            }
        };

    }


    public void pairMyo(View view) {
        Log.d(TAG, "Searching for Myo");
        Intent intent = new Intent(this, ScanActivity.class);
        this.startActivity(intent);
    }

    public VlcFragment getVlcFragment() {
        return (VlcFragment) getFragmentManager().findFragmentByTag(getFragmentTag(1));
    }

    public void findVLC(View view) {
        Log.d(TAG, "Searching for VLC");
        getVlcFragment().mService.startSweep();
    }

    public void connectVLC(View view) {
        getVlcFragment().connect();
    }

    public void clickVlcFragment(View view) {
        getVlcFragment().click(view);
    }
}

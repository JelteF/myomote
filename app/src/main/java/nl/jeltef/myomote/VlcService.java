package nl.jeltef.myomote;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class VlcService extends Service implements PortSweeper.Callback {
    public enum Action {
        TOGGLE_PLAY, CONNECT, SWEEP, NEXT, PREVIOUS
    }

    private final static String TAG = "VlcService";

    public String mHostName;
    public String mPassword;
    private String mBaseUrl;
    private String mSeekTime = "10";

    private boolean mConnected = false;
    private long mLastRequest = 0;
    private int mVolumeDifferenceTotal = 0;

    private final static long REQUEST_DISTANCE = 60000000;
    public Callback mCallback = null;

    /**
     * Begin GPLv3 code
     */
    private PortSweeper mPortSweeper;
    private String mPath;
    private int mPort;
    private int mWorkers;
    public static final int DEFAULT_WORKERS = 16;
    /**
     * End GPLv3 code
     */

    private final IBinder mBinder = new LocalBinder();


    public interface Callback {
        public void onUpdate(JSONObject json);
        public void onHostFound(String hostname);
        public void onConnected();
        public void onDisconnected();
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        VlcService getService() {
            return VlcService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding");
        Log.d(TAG, "" + mConnected);
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    @Override
    public void onCreate() {
        Log.d(TAG, "Creating portsweeper");
        mPort = 8080;
        mPath = "/requests/";
        mWorkers = DEFAULT_WORKERS;

        mPortSweeper = createPortSweeper(this);

    }


    @Override
    public void onHostFound(String hostname, int responseCode) {
        mHostName = hostname;
        mCallback.onHostFound(hostname);
        Log.d(TAG, "Found at " + hostname + ":8080");
        mPortSweeper.abort();
    }

    @Override
    public void onProgress(int progress, int max) {
        if (progress == max) {
            Log.d(TAG, "Finished sweeping");

        }
    }

    /**
     * @return current connection state
     */
    public boolean addCallback(Callback callback) {
        mCallback = callback;
        Log.d(TAG, "Callback value: " + mConnected);
        return mConnected;
    }

    public boolean requestAllowed() {
        return (mLastRequest + REQUEST_DISTANCE) < System.nanoTime();
    }

    public void connect(String password) {
        //TODO: Broadcast something
        mPassword = Uri.encode(password);
        mBaseUrl = "http://" + mHostName + ":" + mPort + mPath;
        mConnected = true;
        if (mCallback != null) {
            mCallback.onConnected();
        }
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

    public void previous() {
        sendStatusCommand("pl_previous", "val=-" + mSeekTime);
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

    public void sendStatusCommand(String command, String params) {
        HttpGet request = new HttpGet(mBaseUrl + "status.json?command=" + command +
                "&" + params);
        request.addHeader(BasicScheme.authenticate(
                new UsernamePasswordCredentials("", mPassword), "UTF-8", false));
        if (mConnected) {
            new SendRequest().execute(request);
        }

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
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                StringBuilder builder = new StringBuilder();
                for (String line = null; (line = reader.readLine()) != null;) {
                    builder.append(line).append("\n");
                }

                JSONTokener tokener = new JSONTokener(builder.toString());
                JSONObject result = new JSONObject(tokener);

                return result;
            } catch (IOException e) {
                //stopCheckingStatus();
                e.printStackTrace();
                return null;
            } catch (JSONException e) {
                //stopCheckingStatus();
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(JSONObject result) {
            if (result == null) {
                mConnected = false;
            }
            if (mCallback != null) {
                if (mConnected) {
                    mCallback.onUpdate(result);
                }
                else {
                    mCallback.onDisconnected();
                }
            }
        }
    }

    /**
     * Begin GPLv3 code
     */
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
    private byte[] getIpAddress() {
        WifiInfo info = getConnectionInfo();
        if (info != null) {
            return toByteArray(info.getIpAddress());
        }
        return null;
    }

    public void startSweep() {
        byte[] ipAddress = getIpAddress();
        Log.d(TAG, "Sweeping for ports");
        if (ipAddress != null) {
            Log.d(TAG, "Sweeping");

            mPortSweeper.sweep(ipAddress);
        }
    }
    /**
     * End GPLv3 code
     */
}

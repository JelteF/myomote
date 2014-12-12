package nl.jeltef.myomote;

import android.app.Application;
import android.content.Intent;

/**
 * Created by jelte on 12-12-14.
 */
public class MyomoteApplication extends Application {
    public void onCreate() {
        startService(new Intent(this, VlcService.class));
    }
}

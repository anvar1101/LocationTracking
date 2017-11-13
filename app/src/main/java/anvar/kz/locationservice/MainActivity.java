package anvar.kz.locationservice;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.location.LocationServices;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static anvar.kz.locationservice.LocationService.ACTION_LOCATION_UPDATE;
import static anvar.kz.locationservice.LocationService.MESSAGE_LOCATION;

public class MainActivity extends AppCompatActivity {

    private final int LOCATION_PERMISSIONS_REQUEST = 1;

    private int attempt = 1;

    private final String TAG = "MainActivity";

    private LocationService mService;
    private boolean mBound = false;

    private ListView list;
    private TextView indicator;

    private Timer timer = null;

    private String[] permissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null != intent && intent.getAction().equals(LocationService.ACTION_LOCATION_UPDATE)) {

                LocationDTO prev = null;

                Location locationData = intent.getParcelableExtra(MESSAGE_LOCATION);
                Log.d("MainActivity", "Latitude: " + locationData.getLatitude() + "Longitude:" + locationData.getLongitude());
                int count = list.getAdapter().getCount();
                float distance = 0f;
                if( count > 0){
                    prev = (LocationDTO) list.getAdapter().getItem(count - 1);
                    distance = locationData.distanceTo(prev.getLocation());
                }
                ((ArrayAdapter<LocationDTO>) list.getAdapter()).add(new LocationDTO(count + 1, locationData, distance));
                ((ArrayAdapter<LocationDTO>) list.getAdapter()).notifyDataSetChanged();
            }
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
    }

    private void run() {
        Log.w(TAG, "run");
        if (!permissionsGranted() && attempt > 3) {
            new AlertDialog.Builder(this)
                    .setTitle("error")
                    .setMessage("impossible work without permissions")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            MainActivity.this.finish();
                        }
                    });
            return;
        } else {
            startService(new Intent(this, LocationService.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        this.list = (ListView) findViewById(R.id.list);
        this.list.setAdapter(new ArrayAdapter<LocationDTO>(this, android.R.layout.simple_list_item_1));
        this.indicator = (TextView) findViewById(R.id.indicator);
        this.run();
    }

    private boolean permissionsGranted() {
        Log.w(TAG, "permissionsGranted");
        boolean found = false;
        for (String s : permissions) {
            if (ContextCompat.checkSelfPermission(this, s) != PackageManager.PERMISSION_GRANTED) {
                found = true;
            }
        }

        if (found) {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSIONS_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.w(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                attempt++;
                run();
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        list = null;
        indicator = null;
        broadcastReceiver = null;
        timer = null;
        super.onDestroy();
    }

    public void stopHandler() {
        timer.cancel();
        timer = null;
    }

    public void startHandler() {
        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {

            synchronized public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mService.getGoogleApiClient().isConnected()) {
                                indicator.setBackgroundColor(Color.GREEN);
                            } else if (mService.getGoogleApiClient().isConnecting()) {
                                indicator.setBackgroundColor(Color.YELLOW);
                            } else {
                                indicator.setBackgroundColor(Color.RED);
                            }

                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                indicator.setText("not grantted");
                                return;
                            } else {
                                Location location = LocationServices.FusedLocationApi.getLastLocation(mService.getGoogleApiClient());
                                indicator.setText(location.getLatitude() + "/" + location.getLongitude());
                            }
                        }catch (Exception ex){
                            indicator.setBackgroundColor(Color.RED);
                            indicator.setText(ex.getLocalizedMessage());
                        }
                    }
                });
            }

        }, TimeUnit.SECONDS.toMillis(3), TimeUnit.SECONDS.toMillis(3));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_LOCATION_UPDATE));

        startHandler();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }


        unregisterReceiver(broadcastReceiver);

        stopHandler();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}

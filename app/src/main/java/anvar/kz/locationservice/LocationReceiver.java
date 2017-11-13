package anvar.kz.locationservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import static anvar.kz.locationservice.LocationService.MESSAGE_LOCATION;


/**
 * Created by Anvar on 13.11.2017.
 */

public class LocationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (null != intent && intent.getAction().equals(LocationService.ACTION_LOCATION_UPDATE)) {
            Location locationData = (Location) intent.getParcelableExtra(MESSAGE_LOCATION);
            Log.d("LocationReceiver", "Latitude: " + locationData.getLatitude() + "Longitude:" + locationData.getLongitude());
        }
    }
}

package anvar.kz.locationservice;

import android.location.Location;

import java.util.Date;

/**
 * Created by Anvar on 14.11.2017.
 */

public class LocationDTO {
    private int index = 0;
    private Location location;
    private Date date;
    private float distance;

    public LocationDTO(int index, Location location, float distance) {
        this.index = index;
        this.location = location;
        this.date = new Date();
        this.distance = distance;
    }

    @Override
    public String toString() {
        return index + "] " + location.getLatitude() + ":" + location.getLongitude() + " [" + this.date.getTime() + "] distance=" + distance;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}

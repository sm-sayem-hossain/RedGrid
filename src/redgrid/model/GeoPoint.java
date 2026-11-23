package redgrid.model;

import java.io.Serializable;

/**
 * A geographic coordinate (WGS-84). Distance between two points is computed
 * with the Haversine formula — the same great-circle math used by map engines.
 */
public class GeoPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Mean Earth radius in kilometers. */
    private static final double EARTH_R_KM = 6371.0088;

    private final double lat;
    private final double lon;

    public GeoPoint(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180)
            throw new IllegalArgumentException("Invalid coordinate: " + lat + "," + lon);
        this.lat = lat;
        this.lon = lon;
    }

    public double lat() { return lat; }
    public double lon() { return lon; }

    /** Great-circle distance to another point, in kilometers. */
    public double distanceKm(GeoPoint other) {
        double dLat = Math.toRadians(other.lat - lat);
        double dLon = Math.toRadians(other.lon - lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_R_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override public String toString() {
        return String.format("%.5f, %.5f", lat, lon);
    }
}

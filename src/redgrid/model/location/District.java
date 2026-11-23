package redgrid.model.location;

import java.io.Serializable;

/**
 * Represents a District in Bangladesh.
 */
public class District implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String bnName;
    private final double lat;
    private final double lon;

    public District(String id, String name, String bnName, double lat, double lon) {
        this.id = id;
        this.name = name;
        this.bnName = bnName;
        this.lat = lat;
        this.lon = lon;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getBnName() { return bnName; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }

    @Override
    public String toString() {
        return name;
    }
}

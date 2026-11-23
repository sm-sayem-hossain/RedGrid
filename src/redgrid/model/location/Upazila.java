package redgrid.model.location;

import java.io.Serializable;

/**
 * Represents an Upazila / Thana under a District.
 */
public class Upazila implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String districtId;
    private final String name;
    private final String bnName;

    public Upazila(String id, String districtId, String name, String bnName) {
        this.id = id;
        this.districtId = districtId;
        this.name = name;
        this.bnName = bnName;
    }

    public String getId() { return id; }
    public String getDistrictId() { return districtId; }
    public String getName() { return name; }
    public String getBnName() { return bnName; }

    @Override
    public String toString() {
        return name;
    }
}

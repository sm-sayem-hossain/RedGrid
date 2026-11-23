package redgrid.model.location;

import java.io.Serializable;

/**
 * Represents a Post Office / Postcode.
 */
public class Postcode implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String districtId;
    private final String upazilaId;
    private final String postOffice;
    private final String code;

    public Postcode(String id, String districtId, String upazilaId, String postOffice, String code) {
        this.id = id;
        this.districtId = districtId;
        this.upazilaId = upazilaId;
        this.postOffice = postOffice;
        this.code = code;
    }

    public String getId() { return id; }
    public String getDistrictId() { return districtId; }
    public String getUpazilaId() { return upazilaId; }
    public String getPostOffice() { return postOffice; }
    public String getCode() { return code; }

    @Override
    public String toString() {
        return code + " - " + postOffice;
    }
}

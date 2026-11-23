package redgrid.model.location;

import java.io.Serializable;

/**
 * Encapsulates the selected address.
 */
public class Address implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String district;
    private final String upazila;
    private final String union;
    private final String postcode;
    private final String hospitalOrDetail;

    public Address(String district, String upazila, String union, String postcode, String hospitalOrDetail) {
        this.district = district != null ? district : "";
        this.upazila = upazila != null ? upazila : "";
        this.union = union != null ? union : "";
        this.postcode = postcode != null ? postcode : "";
        this.hospitalOrDetail = hospitalOrDetail != null ? hospitalOrDetail : "";
    }

    public String getDistrict() { return district; }
    public String getUpazila() { return upazila; }
    public String getUnion() { return union; }
    public String getPostcode() { return postcode; }
    public String getHospitalOrDetail() { return hospitalOrDetail; }

    /**
     * Formats address for display.
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (!hospitalOrDetail.isEmpty()) sb.append(hospitalOrDetail).append(", ");
        if (!union.isEmpty()) sb.append(union).append(", ");
        if (!upazila.isEmpty()) sb.append(upazila).append(", ");
        if (!district.isEmpty()) sb.append(district);
        if (!postcode.isEmpty()) sb.append(" - ").append(postcode);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedAddress();
    }
}

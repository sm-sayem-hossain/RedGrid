package redgrid.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An emergency blood request filed by a recipient. For every compatible,
 * eligible donor we track the alert response (PENDING / ACCEPTED / DECLINED).
 */
public class BloodRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status { OPEN, FULFILLED, CLOSED }
    public enum Response { PENDING, ACCEPTED, DECLINED }

    private final String id;
    private final String recipientId;
    private final String recipientName;
    private final BloodGroup bloodGroup;
    private final int bags;
    private final String hospital;
    private final String district;
    private final GeoPoint location;
    private redgrid.model.location.Address fullAddress;
    private final boolean urgent;
    private final LocalDateTime createdAt;
    private Status status = Status.OPEN;
    /** donorId -> alert response */
    private final Map<String, Response> responses = new LinkedHashMap<>();

    public BloodRequest(String id, User requester, BloodGroup bloodGroup, int bags,
                        String hospital, String district, GeoPoint location, boolean urgent) {
        this(id, requester, bloodGroup, bags, hospital, district, location, null, urgent);
    }

    public BloodRequest(String id, User requester, BloodGroup bloodGroup, int bags,
                        String hospital, String district, GeoPoint location,
                        redgrid.model.location.Address fullAddress, boolean urgent) {
        this.id = id;
        this.recipientId = requester.getId();
        this.recipientName = requester.getName();
        this.bloodGroup = bloodGroup;
        this.bags = bags;
        this.hospital = hospital;
        this.district = district;
        this.location = location;
        this.fullAddress = fullAddress;
        this.urgent = urgent;
        this.createdAt = LocalDateTime.now();
    }

    public String getId()            { return id; }
    public String getRecipientId()   { return recipientId; }
    public String getRecipientName() { return recipientName; }
    public BloodGroup getBloodGroup(){ return bloodGroup; }
    public int getBags()             { return bags; }
    public String getHospital()      { return hospital; }
    public String getDistrict()      { return district; }
    public GeoPoint getLocation()    { return location; }
    public redgrid.model.location.Address getFullAddress() { return fullAddress; }
    public boolean isUrgent()        { return urgent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Status getStatus()        { return status; }

    public void setStatus(Status s)  { this.status = s; }

    public Map<String, Response> getResponses() { return responses; }

    /** Seeds an alert for a matched donor; donors already on the list are untouched. */
    public void addAlertedDonor(String donorId) {
        responses.putIfAbsent(donorId, Response.PENDING);
    }

    public Response responseOf(String donorId) {
        return responses.getOrDefault(donorId, Response.PENDING);
    }

    public int acceptedCount() {
        return (int) responses.values().stream().filter(r -> r == Response.ACCEPTED).count();
    }

    @Override public String toString() {
        return id + " | " + bloodGroup + " x" + bags + " | " + hospital + " | " + status;
    }
}

package redgrid.model;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * An immutable audit entry: one completed donation, written when a donor
 * accepts an emergency request and confirms the handover.
 */
public class DonationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String donorId;
    private final String donorName;
    private final String recipientName;
    private final BloodGroup bloodGroup;
    private final String hospital;
    private final String district;
    private final LocalDate date;

    public DonationRecord(String id, Donor donor, BloodRequest request) {
        this.id = id;
        this.donorId = donor.getId();
        this.donorName = donor.getName();
        this.recipientName = request.getRecipientName();
        this.bloodGroup = donor.getBloodGroup();
        this.hospital = request.getHospital();
        this.district = request.getDistrict();
        this.date = LocalDate.now();
    }

    public String getId()            { return id; }
    public String getDonorId()       { return donorId; }
    public String getDonorName()     { return donorName; }
    public String getRecipientName() { return recipientName; }
    public BloodGroup getBloodGroup(){ return bloodGroup; }
    public String getHospital()      { return hospital; }
    public String getDistrict()      { return district; }
    public LocalDate getDate()       { return date; }
}

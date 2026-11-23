package redgrid.model;

import java.io.Serializable;

/** An account that searches for blood and files emergency requests. */
public class Recipient extends User implements Serializable {

    private static final long serialVersionUID = 1L;

    private BloodGroup bloodGroup;

    public Recipient(String id, String name, String phone, String password,
                     String district, BloodGroup bloodGroup) {
        super(id, name, phone, password, district, Role.RECIPIENT);
        this.bloodGroup = bloodGroup;
    }

    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup g) { this.bloodGroup = g; }
}

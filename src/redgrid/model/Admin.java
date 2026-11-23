package redgrid.model;

import java.io.Serializable;

/** System administrator: audits users, donations and emergency matching. */
public class Admin extends User implements Serializable {

    private static final long serialVersionUID = 1L;

    public Admin(String id, String name, String phone, String password, String district) {
        super(id, name, phone, password, district, Role.ADMIN);
    }
}

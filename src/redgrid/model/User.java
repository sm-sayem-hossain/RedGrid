package redgrid.model;

import java.io.Serializable;

/**
 * Abstract base of every RedGrid account. Holds the shared identity fields;
 * specialized subclasses (Donor, Recipient, Admin) extend it with role data.
 */
public abstract class User implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Role { DONOR, RECIPIENT, ADMIN }

    private String id;
    private String name;
    private String phone;
    private String password;
    private String district;
    private GeoPoint location;
    private Role role;

    protected User(String id, String name, String phone, String password, String district, Role role) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.password = password;
        this.district = district;
        this.role = role;
    }

    public String getId()       { return id; }
    public String getName()     { return name; }
    public String getPhone()    { return phone; }
    public String getDistrict() { return district; }
    public GeoPoint getLocation() { return location; }
    public Role getRole()       { return role; }

    /** Package-private read used by the authentication service only. */
    String getPassword() { return password; }

    void setPassword(String password) { this.password = password; }

    public void setName(String name)         { this.name = name; }
    public void setPhone(String phone)       { this.phone = phone; }
    public void setDistrict(String district) { this.district = district; }
    public void setLocation(GeoPoint location) { this.location = location; }

    /** Returns true when the given plain-text password matches this account. */
    public boolean checkPassword(String attempt) { return password != null && password.equals(attempt); }

    @Override public String toString() { return name + " (" + id + ")"; }
}

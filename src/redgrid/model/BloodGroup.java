package redgrid.model;

import java.io.Serializable;

/**
 * The eight human ABO/Rh blood groups with full medical receive-compatibility.
 * A donor group can give to a recipient group if and only if
 * {@code recipient.canReceiveFrom(donor)} is true.
 */
public enum BloodGroup implements Serializable {
    O_NEG("O-"), O_POS("O+"),
    A_NEG("A-"), A_POS("A+"),
    B_NEG("B-"), B_POS("B+"),
    AB_NEG("AB-"), AB_POS("AB+");

    private final String label;

    BloodGroup(String label) { this.label = label; }

    public String label() { return label; }

    public static BloodGroup fromLabel(String label) {
        for (BloodGroup g : values()) if (g.label.equals(label)) return g;
        throw new IllegalArgumentException("Unknown blood group: " + label);
    }

    @Override public String toString() { return label; }

    /** True when a donor with this group may safely give blood to {@code recipient}. */
    public boolean canDonateTo(BloodGroup recipient) {
        return recipient.canReceiveFrom(this);
    }

    /** True when a recipient with this group may safely receive from {@code donor}. */
    public boolean canReceiveFrom(BloodGroup donor) {
        switch (this) {
            case AB_POS: return true;                                  // universal recipient
            case AB_NEG: return donor == O_NEG || donor == A_NEG || donor == B_NEG || donor == AB_NEG;
            case A_POS:  return donor == O_NEG || donor == O_POS || donor == A_NEG || donor == A_POS;
            case A_NEG:  return donor == O_NEG || donor == A_NEG;
            case B_POS:  return donor == O_NEG || donor == O_POS || donor == B_NEG || donor == B_POS;
            case B_NEG:  return donor == O_NEG || donor == B_NEG;
            case O_POS:  return donor == O_NEG || donor == O_POS;
            case O_NEG:  return donor == O_NEG;                        // universal donor only
            default:     return false;
        }
    }
}

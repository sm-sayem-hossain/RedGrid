package redgrid.model;

/**
 * Central place for the medical rest-period rule shared by model and UI layers.
 */
public final class EligibilityValidator {

    /** Mandatory minimum recovery interval between whole-blood donations. */
    public static final long REST_DAYS = 90;

    private EligibilityValidator() {}

    public static boolean check90Days(Donor donor) { return donor.isEligible(); }
}

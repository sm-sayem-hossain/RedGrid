package redgrid.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A voluntary blood donor. Carries blood group + last donation date; the
 * mandatory 90-day rest rule is enforced through {@link #daysUntilEligible()}.
 */
public class Donor extends User implements Serializable {

    private static final long serialVersionUID = 1L;

    private BloodGroup bloodGroup;
    private LocalDate lastDonation;      // null = never donated
    private boolean availableAsDonor;    // user must explicitly opt-in to receive alerts

    public Donor(String id, String name, String phone, String password,
                 String district, BloodGroup bloodGroup) {
        super(id, name, phone, password, district, Role.DONOR);
        this.bloodGroup = bloodGroup;
    }

    public BloodGroup getBloodGroup()  { return bloodGroup; }
    public void setBloodGroup(BloodGroup g) { this.bloodGroup = g; }

    /** Whether the user has opted in to being a donor and receiving emergency alerts. */
    public boolean isAvailableAsDonor() { return availableAsDonor; }
    public void setAvailableAsDonor(boolean available) { this.availableAsDonor = available; }

    public LocalDate getLastDonation() { return lastDonation; }

    /** Records a donation made today. */
    public void recordDonationToday() { lastDonation = LocalDate.now(); }

    public void setLastDonation(LocalDate d) { lastDonation = d; }

    /** Days elapsed since the last donation (Long.MAX_VALUE when never donated). */
    public long daysSinceLastDonation() {
        return lastDonation == null ? Long.MAX_VALUE
               : ChronoUnit.DAYS.between(lastDonation, LocalDate.now());
    }

    /** True when the mandatory 90-day recovery interval has fully elapsed. */
    public boolean isEligible() { return daysSinceLastDonation() >= EligibilityValidator.REST_DAYS; }

    /** Days remaining before the donor becomes eligible again (0 when eligible). */
    public long daysUntilEligible() {
        if (isEligible()) return 0;
        return REST_MS_DAYS - daysSinceLastDonation();
    }

    private static final long REST_MS_DAYS = EligibilityValidator.REST_DAYS;

    public String eligibilityText() {
        return isEligible() ? "Eligible" : "Resting (" + daysUntilEligible() + "d left)";
    }
}

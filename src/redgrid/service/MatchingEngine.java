package redgrid.service;

import redgrid.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministic proximity matcher: ABO/Rh safety + 90-day cooldown +
 * Haversine distance within the alert radius. Nearest donor comes first.
 */
public final class MatchingEngine {

    /** Donors farther than this from the request are never alerted (km). */
    public static final double ALERT_RADIUS_KM = 25.0;

    private MatchingEngine() {}

    /**
     * Returns every eligible, medically compatible donor within the alert
     * radius of the request, ordered nearest-first. Same input always yields
     * the same ordering (distance, then id as tiebreaker).
     */
    public static List<Donor> findDonors(BloodRequest req) {
        List<Donor> result = new ArrayList<>();
        for (Donor d : Store.donors()) {
            if (!d.isAvailableAsDonor()) continue;           // skip users who haven't opted in
            if (!evaluateABO_Rh(d.getBloodGroup(), req.getBloodGroup())) continue;
            if (!EligibilityValidator.check90Days(d)) continue;
            if (d.getLocation() == null || req.getLocation() == null) continue;
            if (d.getLocation().distanceKm(req.getLocation()) <= ALERT_RADIUS_KM) result.add(d);
        }
        result.sort(Comparator
            .comparingDouble((Donor d) -> d.getLocation().distanceKm(req.getLocation()))
            .thenComparing(User::getId));
        return result;
    }

    /** Pure ABO/Rh medical compatibility check. */
    public static boolean evaluateABO_Rh(BloodGroup donor, BloodGroup recipient) {
        return donor.canDonateTo(recipient);
    }

    /** Great-circle distance between a donor and a request, in kilometers. */
    public static double distanceKm(Donor d, BloodRequest req) {
        if (d.getLocation() == null || req.getLocation() == null) return Double.MAX_VALUE;
        return d.getLocation().distanceKm(req.getLocation());
    }
}

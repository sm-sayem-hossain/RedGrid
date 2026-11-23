package redgrid.model;

import java.util.List;

/**
 * Curated catalogue of Bangladeshi hospitals and city centres with real
 * coordinates — this is what users pick instead of typing an address.
 */
public final class LocationCatalog {

    public record Place(String name, String district, double lat, double lon) {
        public GeoPoint geo() { return new GeoPoint(lat, lon); }
        @Override public String toString() { return name + " · " + district; }
    }

    public static final List<Place> PLACES = List.of(
        new Place("Dhaka Medical College Hospital",   "Dhaka",       23.7259,  90.3973),
        new Place("Square Hospital",                  "Dhaka",       23.7529,  90.3776),
        new Place("BSMMU",                            "Dhaka",       23.7386,  90.3958),
        new Place("United Hospital",                  "Dhaka",       23.8022,  90.4152),
        new Place("Evercare Hospital",                "Dhaka",       23.8103,  90.4350),
        new Place("Kurmitola General Hospital",       "Dhaka",       23.8340,  90.4040),
        new Place("Narayanganj 300-bed Hospital",     "Narayanganj", 23.6205,  90.4996),
        new Place("Gazipur City Hospital",            "Gazipur",     23.9999,  90.4203),
        new Place("Cumilla Medical College Hospital", "Cumilla",     23.4645,  91.1780),
        new Place("Chattogram Medical College",       "Chattogram",  22.3616,  91.8318),
        new Place("Cox's Bazar Sadar Hospital",       "Cox's Bazar", 21.4400,  92.0060),
        new Place("Sylhet MAG Osmani Medical College","Sylhet",      24.8990,  91.8697),
        new Place("Mymensingh Medical College",       "Mymensingh",  24.7508,  90.3990),
        new Place("Rajshahi Medical College",         "Rajshahi",    24.3675,  88.6032),
        new Place("Bogura Shaheed Ziaur Rahman Med.", "Bogura",      24.8481,  89.3720),
        new Place("Khulna Medical College",           "Khulna",      22.8137,  89.5427),
        new Place("Jashore 250-bed Hospital",         "Jashore",     23.1665,  89.2080),
        new Place("Barishal Sher-e-Bangla Medical",   "Barishal",    22.7000,  90.3515),
        new Place("Rangpur Medical College",          "Rangpur",     25.7439,  89.2753),
        new Place("Dinajpur M Abdur Rahim Med.",      "Dinajpur",    25.6200,  88.6350)
    );

    private LocationCatalog() {}

    public static String[] names() {
        return PLACES.stream().map(Place::toString).toArray(String[]::new);
    }

    /** Finds a place by its display string ("Name · District"). */
    public static Place byName(String display) {
        for (Place p : PLACES) if (p.toString().equals(display)) return p;
        return PLACES.get(0);
    }
}

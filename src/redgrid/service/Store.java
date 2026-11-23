package redgrid.service;

import redgrid.model.*;
import redgrid.persistence.FilePersistenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Central in-memory registry of every RedGrid entity, backed by the
 * FilePersistenceManager. The UI always reads from / mutates through here.
 */
public final class Store {

    private static final List<User> users = new ArrayList<>();
    private static final List<BloodRequest> requests = new ArrayList<>();
    private static final List<DonationRecord> records = new ArrayList<>();

    private static int nextUserId = 1;
    private static int nextRequestId = 1;
    private static int nextRecordId = 1;

    private static final Pattern PHONE = Pattern.compile("^01[3-9]\\d{8}$");

    static { load(); }

    private Store() {}

    // ---------- boot ----------

    @SuppressWarnings("unchecked")
    private static void load() {
        List<User> loadedUsers = (List<User>) (List<?>) FilePersistenceManager.read("users.dat");
        users.addAll(loadedUsers);
        List<BloodRequest> loadedReq = (List<BloodRequest>) (List<?>) FilePersistenceManager.read("requests.dat");
        requests.addAll(loadedReq);
        List<DonationRecord> loadedRec = (List<DonationRecord>) (List<?>) FilePersistenceManager.read("records.dat");
        records.addAll(loadedRec);
        bumpCounters();
        if (users.stream().noneMatch(u -> u.getRole() == User.Role.ADMIN)) seedAdmin();
    }

    private static void bumpCounters() {
        for (User u : users) {
            if (u.getId().startsWith("D-")) nextUserId = Math.max(nextUserId, Integer.parseInt(u.getId().substring(2)) + 1);
        }
        for (BloodRequest r : requests) {
            nextRequestId = Math.max(nextRequestId, Integer.parseInt(r.getId().substring(2)) + 1);
        }
        for (DonationRecord d : records) {
            nextRecordId = Math.max(nextRecordId, Integer.parseInt(d.getId().substring(2)) + 1);
        }
    }

    /** Built-in administrator so the admin console is reachable on first run. */
    private static void seedAdmin() {
        Admin a = new Admin("A-0001", "System Admin", "01700000000", "admin", "Dhaka");
        a.setLocation(new GeoPoint(23.7259, 90.3973));
        users.add(a);
        saveUsers();
    }

    // ---------- persistence ----------

    public static void saveUsers()    { FilePersistenceManager.write("users.dat", users); }
    public static void saveRequests() { FilePersistenceManager.write("requests.dat", requests); }
    public static void saveRecords()  { FilePersistenceManager.write("records.dat", records); }
    public static void saveAll()      { saveUsers(); saveRequests(); saveRecords(); }

    // ---------- users ----------

    public static List<User> getUsers()      { return users; }
    public static List<BloodRequest> getRequests() { return requests; }
    public static List<DonationRecord> getRecords() { return records; }

    public static List<Donor> donors() {
        List<Donor> list = new ArrayList<>();
        for (User u : users) if (u instanceof Donor d) list.add(d);
        return list;
    }

    public static Donor findDonor(String id) {
        for (User u : users) if (u.getId().equals(id) && u instanceof Donor d) return d;
        return null;
    }

    public static User findById(String id) {
        for (User u : users) if (u.getId().equals(id)) return u;
        return null;
    }

    // ---------- validation ----------

    /** Bangladeshi mobile pattern 01[3-9] + 8 digits. */
    public static boolean validPhone(String phone) {
        return phone != null && PHONE.matcher(phone).matches();
    }

    /** Registers a member account (no location yet — set from the profile map). */
    public static String registerUser(String name, String phone, String password, BloodGroup group) {
        String err = validate(name, phone, password);
        if (err != null) return err;
        if (phoneTaken(phone)) return "This phone number is already registered.";
        Donor d = new Donor(nextId("U"), name.trim(), phone, password, "—", group);
        users.add(d);
        saveUsers();
        return null;
    }

    private static String validate(String name, String phone, String password) {
        if (name == null || name.isBlank()) return "Name is required.";
        if (!validPhone(phone)) return "Phone must be a valid BD number (e.g. 01712345678).";
        if (password == null || password.length() < 4) return "Password must be at least 4 characters.";
        return null;
    }

    private static boolean phoneTaken(String phone) {
        return users.stream().anyMatch(u -> u.getPhone().equals(phone));
    }

    private static String nextId(String prefix) {
        return String.format("%s-%04d", prefix, nextUserId++);
    }

    private static String nextRequestId() {
        return String.format("RQ-%04d", nextRequestId++);
    }

    private static String nextRecordId() {
        return String.format("DN-%04d", nextRecordId++);
    }

    // ---------- auth ----------

    /** Verifies credentials and returns the account, or null on failure. */
    public static User verifyCredentials(String idOrPhone, String password) {
        for (User u : users) {
            if ((u.getId().equalsIgnoreCase(idOrPhone) || u.getPhone().equals(idOrPhone))
                    && u.checkPassword(password)) {
                return u;
            }
        }
        return null;
    }

    // ---------- requests ----------

    /** Creates a request, runs the matching engine and seeds donor alerts. */
    public static BloodRequest createRequest(User requester, BloodGroup group, int bags,
                                             LocationCatalog.Place place, boolean urgent) {
        BloodRequest req = new BloodRequest(nextRequestId(), requester, group, bags,
                place.name(), place.district(), place.geo(), urgent);
        List<Donor> matches = MatchingEngine.findDonors(req);
        for (Donor d : matches) {
            if (d.getId().equals(requester.getId())) continue; // never alert the requester
            req.addAlertedDonor(d.getId());
        }
        requests.add(req);
        saveRequests();
        return req;
    }

    /** Creates a request with cascading address, runs matching engine and alerts donors. */
    public static BloodRequest createRequest(User requester, BloodGroup group, int bags,
                                             redgrid.model.location.Address addr,
                                             GeoPoint geo, boolean urgent) {
        String hospital = addr.getHospitalOrDetail().isEmpty() ? "Hospital" : addr.getHospitalOrDetail();
        BloodRequest req = new BloodRequest(nextRequestId(), requester, group, bags,
                hospital, addr.getDistrict(), geo, addr, urgent);
        List<Donor> matches = MatchingEngine.findDonors(req);
        for (Donor d : matches) {
            if (d.getId().equals(requester.getId())) continue;
            req.addAlertedDonor(d.getId());
        }
        requests.add(req);
        saveRequests();
        return req;
    }

    /** Registers a donor's accept/decline decision; a fulfilled request closes. */
    public static void setResponse(BloodRequest req, Donor donor, BloodRequest.Response resp) {
        req.getResponses().put(donor.getId(), resp);
        if (resp == BloodRequest.Response.ACCEPTED) {
            donor.recordDonationToday();
            records.add(new DonationRecord(nextRecordId(), donor, req));
            if (req.acceptedCount() >= req.getBags()) req.setStatus(BloodRequest.Status.FULFILLED);
            saveRecords();
            saveUsers();
        }
        saveRequests();
    }
}

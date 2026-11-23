package redgrid.service;

import redgrid.model.location.District;
import redgrid.model.location.Postcode;
import redgrid.model.location.Union;
import redgrid.model.location.Upazila;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads Bangladesh geocode data from local JSON files without any external library.
 * Provides fast filtered lists for District, Upazila, Union, and Postcode.
 */
public class LocationDataManager {

    private static final LocationDataManager INSTANCE = new LocationDataManager();

    private final List<District> districts = new ArrayList<>();
    private final Map<String, List<Upazila>> upazilasByDistrict = new HashMap<>();
    private final Map<String, List<Union>> unionsByUpazila = new HashMap<>();
    private final Map<String, List<Postcode>> postcodesByDistrict = new HashMap<>();
    private final Map<String, List<Postcode>> postcodesByUpazila = new HashMap<>();

    private boolean loaded = false;

    private LocationDataManager() {
        loadData();
    }

    public static LocationDataManager getInstance() {
        return INSTANCE;
    }

    /**
     * Reads and parses districts, upazilas, unions, and postcodes.
     */
    public synchronized void loadData() {
        if (loaded) return;
        try {
            loadDistricts(new File("data/geo/districts.json"));
            loadUpazilas(new File("data/geo/upazilas.json"));
            loadUnions(new File("data/geo/unions.json"));
            loadPostcodes(new File("data/geo/postcodes.json"));
            loaded = true;
        } catch (Exception e) {
            System.err.println("Failed to load geo data: " + e.getMessage());
        }
    }

    private void loadDistricts(File file) throws IOException {
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("{\"id\":")) {
                    String id = extractValue(line, "id");
                    String name = extractValue(line, "name");
                    String bnName = extractValue(line, "bn_name");
                    double lat = parseDouble(extractValue(line, "lat"));
                    double lon = parseDouble(extractValue(line, "lon"));
                    if (id != null && name != null) {
                        districts.add(new District(id, name, bnName, lat, lon));
                    }
                }
            }
        }
        districts.sort(Comparator.comparing(District::getName));
    }

    private void loadUpazilas(File file) throws IOException {
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("{\"id\":")) {
                    String id = extractValue(line, "id");
                    String districtId = extractValue(line, "district_id");
                    String name = extractValue(line, "name");
                    String bnName = extractValue(line, "bn_name");
                    if (id != null && districtId != null && name != null) {
                        Upazila upazila = new Upazila(id, districtId, name, bnName);
                        upazilasByDistrict.computeIfAbsent(districtId, k -> new ArrayList<>()).add(upazila);
                    }
                }
            }
        }
        for (List<Upazila> list : upazilasByDistrict.values()) {
            list.sort(Comparator.comparing(Upazila::getName));
        }
    }

    private void loadUnions(File file) throws IOException {
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("{\"id\":")) {
                    String id = extractValue(line, "id");
                    String upazilaId = extractValue(line, "upazilla_id");
                    String name = extractValue(line, "name");
                    String bnName = extractValue(line, "bn_name");
                    if (id != null && upazilaId != null && name != null) {
                        Union union = new Union(id, upazilaId, name, bnName);
                        unionsByUpazila.computeIfAbsent(upazilaId, k -> new ArrayList<>()).add(union);
                    }
                }
            }
        }
        for (List<Union> list : unionsByUpazila.values()) {
            list.sort(Comparator.comparing(Union::getName));
        }
    }

    private void loadPostcodes(File file) throws IOException {
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("{\"id\":")) {
                    String id = extractValue(line, "id");
                    String districtId = extractValue(line, "district_id");
                    String upazilaId = extractValue(line, "upazila_id");
                    String postOffice = extractValue(line, "post_office");
                    String code = extractValue(line, "postcode");
                    if (id != null && code != null) {
                        Postcode p = new Postcode(id, districtId, upazilaId, postOffice, code);
                        if (districtId != null && !districtId.isEmpty()) {
                            postcodesByDistrict.computeIfAbsent(districtId, k -> new ArrayList<>()).add(p);
                        }
                        if (upazilaId != null && !upazilaId.isEmpty() && !"null".equals(upazilaId)) {
                            postcodesByUpazila.computeIfAbsent(upazilaId, k -> new ArrayList<>()).add(p);
                        }
                    }
                }
            }
        }
    }

    /**
     * Simple key-value string extractor for JSON lines.
     * e.g., line containing "name":"Dhaka" returns Dhaka.
     */
    private String extractValue(String line, String key) {
        String pattern = "\"" + key + "\":";
        int start = line.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();

        if (start < line.length() && line.charAt(start) == '"') {
            start++;
            int end = line.indexOf('"', start);
            if (end == -1) return null;
            return line.substring(start, end);
        } else {
            int endComma = line.indexOf(',', start);
            int endBrace = line.indexOf('}', start);
            int end = line.length();
            if (endComma != -1 && endComma < end) end = endComma;
            if (endBrace != -1 && endBrace < end) end = endBrace;
            String val = line.substring(start, end).trim();
            return "null".equals(val) ? null : val;
        }
    }

    private double parseDouble(String str) {
        if (str == null || str.isEmpty() || "null".equals(str)) return 0.0;
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ---------- Query API ----------

    public List<District> getDistricts() {
        return Collections.unmodifiableList(districts);
    }

    public List<Upazila> getUpazilas(String districtId) {
        if (districtId == null) return Collections.emptyList();
        return upazilasByDistrict.getOrDefault(districtId, Collections.emptyList());
    }

    public List<Union> getUnions(String upazilaId) {
        if (upazilaId == null) return Collections.emptyList();
        return unionsByUpazila.getOrDefault(upazilaId, Collections.emptyList());
    }

    public List<Postcode> getPostcodes(String districtId, String upazilaId) {
        if (upazilaId != null && postcodesByUpazila.containsKey(upazilaId)) {
            return postcodesByUpazila.get(upazilaId);
        }
        if (districtId != null && postcodesByDistrict.containsKey(districtId)) {
            return postcodesByDistrict.get(districtId);
        }
        return Collections.emptyList();
    }
}

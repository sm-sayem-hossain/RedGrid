package redgrid.ui;

import redgrid.model.location.Address;
import redgrid.model.location.District;
import redgrid.model.location.Postcode;
import redgrid.model.location.Union;
import redgrid.model.location.Upazila;
import redgrid.service.LocationDataManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

/**
 * Reusable cascading dropdown panel for selecting Bangladesh addresses:
 * District -> Upazila -> Union -> Postcode.
 * Features:
 * - Clear placeholders like "-- Select District --"
 * - Spacious vertical box heights (46px) with clean typography
 * - Clean borders and comfortable padding
 */
public class AddressSelectorPanel extends JPanel {

    private final JComboBox<Object> districtCombo = new JComboBox<>();
    private final JComboBox<Object> upazilaCombo = new JComboBox<>();
    private final JComboBox<Object> unionCombo = new JComboBox<>();
    private final JComboBox<Object> postcodeCombo = new JComboBox<>();
    private final JTextField hospitalField = Theme.textField();
    private final JLabel hospitalLabel;
    private final boolean showHospitalField;

    private static final String PLACEHOLDER_DISTRICT = "-- Select District (জেলা নির্বাচন করুন) --";
    private static final String PLACEHOLDER_UPAZILA  = "-- Select Upazila / Thana (উপজেলা নির্বাচন করুন) --";
    private static final String PLACEHOLDER_UNION    = "-- Select Union / Ward (ইউনিয়ন নির্বাচন করুন) --";
    private static final String PLACEHOLDER_POSTCODE = "-- Select Postcode (পোস্ট কোড) --";

    public AddressSelectorPanel(boolean showHospitalField, String hospitalLabelText) {
        this.showHospitalField = showHospitalField;
        this.hospitalLabel = Theme.label(hospitalLabelText != null ? hospitalLabelText : "Hospital / Clinic name",
                Theme.sans(Font.BOLD, 13), Theme.INK);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        setupComboBoxStyles();
        buildUI();
        initListeners();
        loadDistricts();
    }

    public AddressSelectorPanel(boolean showHospitalField) {
        this(showHospitalField, "Hospital / Clinic name");
    }

    private void setupComboBoxStyles() {
        styleCombo(districtCombo);
        styleCombo(upazilaCombo);
        styleCombo(unionCombo);
        styleCombo(postcodeCombo);
    }

    private void styleCombo(JComboBox<?> box) {
        box.setFont(Theme.sans(Font.PLAIN, 14));
        box.setBackground(Theme.SURFACE);
        box.setForeground(Theme.INK);
        box.setPreferredSize(new Dimension(300, 46));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        box.setMinimumSize(new Dimension(200, 46));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setBorder(new CompoundBorder(
            new LineBorder(Theme.LINE_STRONG, 1),
            new EmptyBorder(4, 8, 4, 8)
        ));
    }

    private void buildUI() {
        add(createSectionLabel("District (জেলা) *"));
        add(Box.createVerticalStrut(6));
        add(districtCombo);
        add(Box.createVerticalStrut(14));

        add(createSectionLabel("Upazila / Thana (উপজেলা/থানা) *"));
        add(Box.createVerticalStrut(6));
        add(upazilaCombo);
        add(Box.createVerticalStrut(14));

        add(createSectionLabel("Union / Ward (ইউনিয়ন/ওয়ার্ড)"));
        add(Box.createVerticalStrut(6));
        add(unionCombo);
        add(Box.createVerticalStrut(14));

        add(createSectionLabel("Postcode (পোস্ট কোড)"));
        add(Box.createVerticalStrut(6));
        add(postcodeCombo);

        if (showHospitalField) {
            add(Box.createVerticalStrut(14));
            add(createSectionLabel("Hospital / Clinic Name *"));
            add(Box.createVerticalStrut(6));
            hospitalField.setFont(Theme.sans(Font.PLAIN, 14));
            hospitalField.setPreferredSize(new Dimension(300, 46));
            hospitalField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            hospitalField.setMinimumSize(new Dimension(200, 46));
            hospitalField.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(hospitalField);
        }
    }

    private JLabel createSectionLabel(String text) {
        JLabel l = Theme.label(text, Theme.sans(Font.BOLD, 13), Theme.INK);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void initListeners() {
        districtCombo.addActionListener(e -> onDistrictSelected());
        upazilaCombo.addActionListener(e -> onUpazilaSelected());
    }

    private void loadDistricts() {
        districtCombo.removeAllItems();
        districtCombo.addItem(PLACEHOLDER_DISTRICT);
        List<District> list = LocationDataManager.getInstance().getDistricts();
        for (District d : list) {
            districtCombo.addItem(d);
        }
        resetUpazilas();
    }

    private void resetUpazilas() {
        upazilaCombo.removeAllItems();
        upazilaCombo.addItem(PLACEHOLDER_UPAZILA);
        resetUnions();
        resetPostcodes();
    }

    private void resetUnions() {
        unionCombo.removeAllItems();
        unionCombo.addItem(PLACEHOLDER_UNION);
    }

    private void resetPostcodes() {
        postcodeCombo.removeAllItems();
        postcodeCombo.addItem(PLACEHOLDER_POSTCODE);
    }

    private void onDistrictSelected() {
        Object item = districtCombo.getSelectedItem();
        resetUpazilas();

        if (item instanceof District selected) {
            List<Upazila> upazilas = LocationDataManager.getInstance().getUpazilas(selected.getId());
            for (Upazila u : upazilas) {
                upazilaCombo.addItem(u);
            }
            loadPostcodes(selected.getId(), null);
        }
    }

    private void onUpazilaSelected() {
        Object item = upazilaCombo.getSelectedItem();
        resetUnions();
        resetPostcodes();

        District selectedDistrict = getSelectedDistrict();
        String distId = selectedDistrict != null ? selectedDistrict.getId() : null;

        if (item instanceof Upazila selectedUpazila) {
            List<Union> unions = LocationDataManager.getInstance().getUnions(selectedUpazila.getId());
            for (Union u : unions) {
                unionCombo.addItem(u);
            }
            loadPostcodes(distId, selectedUpazila.getId());
        } else if (distId != null) {
            loadPostcodes(distId, null);
        }
    }

    private void loadPostcodes(String districtId, String upazilaId) {
        postcodeCombo.removeAllItems();
        postcodeCombo.addItem(PLACEHOLDER_POSTCODE);
        List<Postcode> list = LocationDataManager.getInstance().getPostcodes(districtId, upazilaId);
        for (Postcode p : list) {
            postcodeCombo.addItem(p);
        }
    }

    // ---------- Public Getters ----------

    public Address getSelectedAddress() {
        District d = getSelectedDistrict();
        Upazila u = getSelectedUpazila();
        Union un = getSelectedUnion();
        Postcode pc = getSelectedPostcode();

        String distName = d != null ? d.getName() : "";
        String upName = u != null ? u.getName() : "";
        String unionName = un != null ? un.getName() : "";
        String pcCode = pc != null ? pc.getCode() : "";
        String detail = showHospitalField ? hospitalField.getText().trim() : "";

        return new Address(distName, upName, unionName, pcCode, detail);
    }

    public District getSelectedDistrict() {
        Object item = districtCombo.getSelectedItem();
        return (item instanceof District) ? (District) item : null;
    }

    public Upazila getSelectedUpazila() {
        Object item = upazilaCombo.getSelectedItem();
        return (item instanceof Upazila) ? (Upazila) item : null;
    }

    public Union getSelectedUnion() {
        Object item = unionCombo.getSelectedItem();
        return (item instanceof Union) ? (Union) item : null;
    }

    public Postcode getSelectedPostcode() {
        Object item = postcodeCombo.getSelectedItem();
        return (item instanceof Postcode) ? (Postcode) item : null;
    }

    public JTextField getHospitalField() {
        return hospitalField;
    }
}

package redgrid.ui;

import redgrid.model.location.Address;
import redgrid.model.location.District;
import redgrid.model.location.Postcode;
import redgrid.model.location.Union;
import redgrid.model.location.Upazila;
import redgrid.service.LocationDataManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Reusable cascading dropdown panel for selecting Bangladesh addresses:
 * District -> Upazila -> Union -> Postcode.
 * Also provides an optional hospital/manual address field.
 */
public class AddressSelectorPanel extends JPanel {

    private final JComboBox<District> districtCombo = new JComboBox<>();
    private final JComboBox<Upazila> upazilaCombo = new JComboBox<>();
    private final JComboBox<Union> unionCombo = new JComboBox<>();
    private final JComboBox<Postcode> postcodeCombo = new JComboBox<>();
    private final JTextField hospitalField = Theme.textField();
    private final JLabel hospitalLabel;
    private final boolean showHospitalField;

    public AddressSelectorPanel(boolean showHospitalField, String hospitalLabelText) {
        this.showHospitalField = showHospitalField;
        this.hospitalLabel = Theme.label(hospitalLabelText != null ? hospitalLabelText : "Hospital / Clinic name",
                Theme.sans(Font.PLAIN, 12), Theme.MUTED);

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
        box.setFont(Theme.sans(Font.PLAIN, 13));
        box.setBackground(Theme.SURFACE);
        box.setForeground(Theme.INK);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void buildUI() {
        add(Theme.label("District (জেলা)", Theme.sans(Font.PLAIN, 12), Theme.MUTED));
        add(Box.createVerticalStrut(4));
        add(districtCombo);
        add(Box.createVerticalStrut(10));

        add(Theme.label("Upazila / Thana (উপজেলা/থানা)", Theme.sans(Font.PLAIN, 12), Theme.MUTED));
        add(Box.createVerticalStrut(4));
        add(upazilaCombo);
        add(Box.createVerticalStrut(10));

        add(Theme.label("Union / Ward (ইউনিয়ন/ওয়ার্ড)", Theme.sans(Font.PLAIN, 12), Theme.MUTED));
        add(Box.createVerticalStrut(4));
        add(unionCombo);
        add(Box.createVerticalStrut(10));

        add(Theme.label("Postcode (পোস্ট কোড)", Theme.sans(Font.PLAIN, 12), Theme.MUTED));
        add(Box.createVerticalStrut(4));
        add(postcodeCombo);

        if (showHospitalField) {
            add(Box.createVerticalStrut(10));
            add(hospitalLabel);
            add(Box.createVerticalStrut(4));
            hospitalField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            hospitalField.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(hospitalField);
        }
    }

    private void initListeners() {
        districtCombo.addActionListener(e -> onDistrictSelected());
        upazilaCombo.addActionListener(e -> onUpazilaSelected());
    }

    private void loadDistricts() {
        districtCombo.removeAllItems();
        List<District> list = LocationDataManager.getInstance().getDistricts();
        for (District d : list) {
            districtCombo.addItem(d);
        }
        if (!list.isEmpty()) {
            districtCombo.setSelectedIndex(0);
        }
    }

    private void onDistrictSelected() {
        District selected = (District) districtCombo.getSelectedItem();
        upazilaCombo.removeAllItems();
        unionCombo.removeAllItems();
        postcodeCombo.removeAllItems();

        if (selected == null) return;

        List<Upazila> upazilas = LocationDataManager.getInstance().getUpazilas(selected.getId());
        for (Upazila u : upazilas) {
            upazilaCombo.addItem(u);
        }
        if (!upazilas.isEmpty()) {
            upazilaCombo.setSelectedIndex(0);
        } else {
            // No upazilas, try postcodes directly for district
            loadPostcodes(selected.getId(), null);
        }
    }

    private void onUpazilaSelected() {
        Upazila selectedUpazila = (Upazila) upazilaCombo.getSelectedItem();
        District selectedDistrict = (District) districtCombo.getSelectedItem();
        unionCombo.removeAllItems();
        postcodeCombo.removeAllItems();

        if (selectedUpazila == null) return;

        List<Union> unions = LocationDataManager.getInstance().getUnions(selectedUpazila.getId());
        for (Union u : unions) {
            unionCombo.addItem(u);
        }
        if (!unions.isEmpty()) {
            unionCombo.setSelectedIndex(0);
        }

        String distId = selectedDistrict != null ? selectedDistrict.getId() : null;
        loadPostcodes(distId, selectedUpazila.getId());
    }

    private void loadPostcodes(String districtId, String upazilaId) {
        postcodeCombo.removeAllItems();
        List<Postcode> list = LocationDataManager.getInstance().getPostcodes(districtId, upazilaId);
        for (Postcode p : list) {
            postcodeCombo.addItem(p);
        }
        if (!list.isEmpty()) {
            postcodeCombo.setSelectedIndex(0);
        }
    }

    // ---------- Public Getters ----------

    public Address getSelectedAddress() {
        District d = (District) districtCombo.getSelectedItem();
        Upazila u = (Upazila) upazilaCombo.getSelectedItem();
        Union un = (Union) unionCombo.getSelectedItem();
        Postcode pc = (Postcode) postcodeCombo.getSelectedItem();

        String distName = d != null ? d.getName() : "";
        String upName = u != null ? u.getName() : "";
        String unionName = un != null ? un.getName() : "";
        String pcCode = pc != null ? pc.getCode() : "";
        String detail = showHospitalField ? hospitalField.getText().trim() : "";

        return new Address(distName, upName, unionName, pcCode, detail);
    }

    public District getSelectedDistrict() {
        return (District) districtCombo.getSelectedItem();
    }

    public JTextField getHospitalField() {
        return hospitalField;
    }
}

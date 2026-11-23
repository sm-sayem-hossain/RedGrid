package redgrid.ui;

import redgrid.model.*;
import redgrid.service.MatchingEngine;
import redgrid.service.Store;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified member dashboard — one account can both file emergency requests
 * and receive donor alerts. Tabs: overview (cooldown + alerts), blood
 * request, and profile with a click-to-set map location.
 */
public class Dashboard extends JFrame {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    private final Donor me;
    private final JLabel eligibility = Theme.label("", Theme.display(Font.BOLD, 30), Theme.INK);
    private final JLabel cooldownMeta = Theme.label("", Theme.mono(11), Theme.DIM);
    private final DefaultListModel<String> myRequests = new DefaultListModel<>();
    private final DefaultListModel<String> alertLog = new DefaultListModel<>();
    private final DefaultListModel<String> pendingModel = new DefaultListModel<>();
    private final java.util.Map<String, BloodRequest> pendingById = new java.util.LinkedHashMap<>();
    private final JList<String> pendingList = new JList<>(pendingModel);
    private final JLabel coordLabel = Theme.label("No location set — tap the map", Theme.mono(12), Theme.DIM);

    public Dashboard(Donor me) {
        super("RedGrid — Dashboard");
        this.me = me;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1020, 700);
        setLocationRelativeTo(null);

        JComponent tabs = TabStrip.build(
            new String[]{"Overview", "Blood request", "Profile & location"},
            new JComponent[]{overviewTab(), requestTab(), profileTab()});

        JPanel root = Theme.panel(Theme.BG);
        root.setLayout(new BorderLayout());
        root.add(ThemeKits.headerBar("Dashboard", me.getName(), me.getId(),
                () -> { dispose(); new LoginFrame(); }), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        refreshStatus();
        refreshRequests();
        refreshAlerts();
        setVisible(true);

        // in-app alert refresh: dispatcher thread nudges the list while logged in
        redgrid.service.AlertDispatcher.notifyObserver(me, r -> SwingUtilities.invokeLater(this::refreshAlerts));
        redgrid.service.AlertDispatcher.start();
    }

    // ---------- overview ----------

    private JPanel overviewTab() {
        JPanel body = Theme.panel(Theme.BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(28, 56, 28, 56));

        JPanel status = ThemeKits.card("Cooldown indicator");
        status.add(eligibility);
        status.add(Box.createVerticalStrut(6));
        status.add(cooldownMeta);
        status.add(Box.createVerticalStrut(14));
        status.add(Theme.label(
            "A mandatory 90-day recovery interval protects donor health between donations.",
            Theme.sans(Font.PLAIN, 13), Theme.MUTED));
        body.add(status);
        body.add(Box.createVerticalStrut(20));

        JPanel alerts = ThemeKits.card("Emergency alerts near you");
        pendingList.setFont(Theme.mono(12));
        pendingList.setBackground(Theme.SURFACE);
        pendingList.setForeground(Theme.INK);
        pendingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(pendingList);
        scroll.setPreferredSize(new Dimension(760, 130));
        scroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        alerts.add(Theme.label("Compatible requests within "
                + String.format("%.0f", MatchingEngine.ALERT_RADIUS_KM)
                + " km — select one, then Accept.", Theme.sans(Font.PLAIN, 13), Theme.MUTED));
        alerts.add(Box.createVerticalStrut(10));
        alerts.add(scroll);
        alerts.add(Box.createVerticalStrut(12));

        JButton accept = Theme.button("Accept", true);
        accept.addActionListener(e -> actOnSelected(true));
        alerts.add(ThemeKits.row(accept));

        alerts.add(Box.createVerticalStrut(16));
        alerts.add(Theme.label("History", Theme.mono(10), Theme.DIM));
        alerts.add(Box.createVerticalStrut(8));
        JList<String> history = new JList<>(alertLog);
        history.setFont(Theme.mono(12));
        history.setBackground(Theme.SURFACE);
        history.setForeground(Theme.INK);
        JScrollPane histScroll = new JScrollPane(history);
        histScroll.setPreferredSize(new Dimension(760, 110));
        histScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        alerts.add(histScroll);
        body.add(alerts);

        JPanel reqCard = ThemeKits.card("My requests");
        JList<String> reqList = new JList<>(myRequests);
        reqList.setFont(Theme.mono(12));
        reqList.setBackground(Theme.SURFACE);
        reqList.setForeground(Theme.INK);
        JScrollPane reqScroll = new JScrollPane(reqList);
        reqScroll.setPreferredSize(new Dimension(760, 140));
        reqScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        reqCard.add(reqScroll);
        body.add(Box.createVerticalStrut(20));
        body.add(reqCard);

        JScrollPane outer = new JScrollPane(body);
        outer.setBorder(null);
        outer.getVerticalScrollBar().setUnitIncrement(16);
        return wrapTab(outer);
    }

    private void refreshStatus() {
        if (me.getLocation() == null) {
            eligibility.setText("Set your location");
            eligibility.setForeground(Theme.ACCENT);
            cooldownMeta.setText("Blood group " + me.getBloodGroup()
                + " · no location set — Profile tab");
            return;
        }
        if (me.isEligible()) {
            eligibility.setText("Eligible to donate");
            eligibility.setForeground(Theme.SUCCESS);
            cooldownMeta.setText("Blood group " + me.getBloodGroup()
                + (me.getLastDonation() == null
                    ? " · no donation logged yet"
                    : " · last donation " + FMT.format(me.getLastDonation()))
                + " · " + me.getLocation());
        } else {
            eligibility.setText("Resting — " + me.daysUntilEligible() + " days left");
            eligibility.setForeground(Theme.ACCENT);
            cooldownMeta.setText("Blood group " + me.getBloodGroup()
                + " · last donation " + FMT.format(me.getLastDonation())
                + " · 90-day recovery in progress · " + me.getLocation());
        }
    }

    private void refreshRequests() {
        myRequests.clear();
        for (BloodRequest r : Store.getRequests()) {
            if (!r.getRecipientId().equals(me.getId())) continue;
            String flag = r.isUrgent() ? "URGENT" : "      ";
            myRequests.addElement(String.format("%s  %s  %s x%d  %s, %s  |  %s  |  accepted %d/%d",
                r.getId(), flag, r.getBloodGroup(), r.getBags(),
                r.getHospital(), r.getDistrict(),
                r.getStatus(), r.acceptedCount(), r.getBags()));
        }
    }

    // ---------- request tab ----------

    private JPanel requestTab() {
        String[] labels = new String[]{"-- Select Blood Group --", "O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"};
        JComboBox<String> group = Theme.comboBox(labels);
        group.setSelectedIndex(0);

        SpinnerNumberModel bagsModel = new SpinnerNumberModel(1, 1, 10, 1);
        JSpinner bags = new JSpinner(bagsModel);
        bags.setFont(Theme.sans(Font.PLAIN, 14));
        bags.setPreferredSize(new Dimension(300, 42));
        bags.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        AddressSelectorPanel addressSelector = new AddressSelectorPanel(true, "Hospital / Clinic Name");

        JCheckBox urgent = new JCheckBox("Urgent (Critical / Emergency requirement)");
        urgent.setFont(Theme.sans(Font.BOLD, 14));
        urgent.setBackground(Theme.SURFACE);
        urgent.setForeground(Theme.ACCENT);
        JButton submit = Theme.button("Broadcast Request", true);
        submit.setPreferredSize(new Dimension(240, 46));

        submit.addActionListener(e -> {
            if (group.getSelectedIndex() <= 0) {
                ThemeKits.error(this, "Please select a valid blood group.");
                return;
            }

            redgrid.model.location.Address addr = addressSelector.getSelectedAddress();
            if (addr.getDistrict().isEmpty()) {
                ThemeKits.error(this, "Please select a district.");
                return;
            }
            if (addr.getUpazila().isEmpty()) {
                ThemeKits.error(this, "Please select an upazila / thana.");
                return;
            }
            if (addr.getHospitalOrDetail().isEmpty()) {
                ThemeKits.error(this, "Please enter the hospital or clinic name.");
                return;
            }

            BloodGroup bg = BloodGroup.fromLabel((String) group.getSelectedItem());
            int n = (Integer) bags.getValue();
            redgrid.model.location.District d = addressSelector.getSelectedDistrict();
            GeoPoint geo = (d != null && d.getLat() != 0) ? new GeoPoint(d.getLat(), d.getLon()) : me.getLocation();

            BloodRequest req = Store.createRequest(me, bg, n, addr, geo, urgent.isSelected());
            int matched = req.getResponses().size();
            ThemeKits.info(this, "Request " + req.getId() + " broadcasted successfully!\nAlerted "
                    + matched + " eligible donor" + (matched == 1 ? "" : "s")
                    + " in " + addr.getDistrict() + ".");
            refreshRequests();
        });

        JPanel form = ThemeKits.card("Emergency Blood Request");
        form.add(Theme.label("Specify blood details and patient hospital location accurately.",
                Theme.sans(Font.PLAIN, 14), Theme.MUTED));
        form.add(Box.createVerticalStrut(18));
        ThemeKits.formRow(form, "Blood Group Needed *", group);
        ThemeKits.formRow(form, "Bags Required (1-10) *", bags);
        form.add(Box.createVerticalStrut(8));
        form.add(addressSelector);
        form.add(Box.createVerticalStrut(14));
        form.add(urgent);
        form.add(Box.createVerticalStrut(18));
        form.add(submit);

        JPanel body = Theme.panel(Theme.BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(28, 56, 36, 56));
        body.add(form);
        body.add(Box.createVerticalGlue());
        return wrapTab(body);
    }

    // ---------- profile tab ----------

    private JPanel profileTab() {
        JPanel card = ThemeKits.card("Your location");
        card.add(Theme.label("Select your area to receive nearby emergency alerts.",
                Theme.sans(Font.PLAIN, 13), Theme.MUTED));
        card.add(Box.createVerticalStrut(14));

        AddressSelectorPanel addressSelector = new AddressSelectorPanel(false);
        card.add(addressSelector);
        card.add(Box.createVerticalStrut(14));

        JLabel currentAddressLabel = Theme.label(
            me.getFullAddress() != null ? "Saved: " + me.getFullAddress() : "No location set yet",
            Theme.mono(12), Theme.DIM
        );
        card.add(currentAddressLabel);
        card.add(Box.createVerticalStrut(14));

        JButton save = Theme.button("Save location", true);
        save.addActionListener(e -> {
            redgrid.model.location.Address addr = addressSelector.getSelectedAddress();
            if (addr.getDistrict().isEmpty()) {
                ThemeKits.error(this, "Please select a district.");
                return;
            }
            redgrid.model.location.District d = addressSelector.getSelectedDistrict();
            if (d != null && d.getLat() != 0) {
                me.setLocation(new GeoPoint(d.getLat(), d.getLon()));
            }
            me.setDistrict(addr.getDistrict());
            me.setFullAddress(addr);
            Store.saveUsers();
            currentAddressLabel.setText("Saved: " + addr);
            refreshStatus();
            ThemeKits.info(this, "Location saved! You will receive alerts for " + addr.getDistrict() + ".");
        });
        card.add(save);
        card.add(Box.createVerticalStrut(16));

        JPanel metaRow = Theme.panel(Theme.SURFACE);
        metaRow.setLayout(new FlowLayout(FlowLayout.LEFT, 24, 4));
        metaRow.add(Theme.label("Name  " + me.getName(), Theme.mono(12), Theme.INK));
        metaRow.add(Theme.label("Phone  " + me.getPhone(), Theme.mono(12), Theme.INK));
        metaRow.add(Theme.label("Blood group  " + me.getBloodGroup(), Theme.mono(12), Theme.INK));
        card.add(metaRow);

        JPanel body = Theme.panel(Theme.BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(28, 56, 28, 56));
        body.add(card);
        return wrapTab(body);
    }

    private JPanel wrapTab(JComponent inner) {
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel wrap = Theme.panel(Theme.BG);
        wrap.setLayout(new BorderLayout());
        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
    }

    // ---------- alerts ----------

    /** Rebuilds the in-app pending alert list (no pop-ups). */
    public void refreshAlerts() {
        pendingModel.clear();
        pendingById.clear();
        for (BloodRequest req : Store.getRequests()) {
            if (req.getStatus() != BloodRequest.Status.OPEN) continue;
            if (req.responseOf(me.getId()) != BloodRequest.Response.PENDING) continue;
            if (!MatchingEngine.evaluateABO_Rh(me.getBloodGroup(), req.getBloodGroup())) continue;
            if (me.getLocation() == null || req.getLocation() == null) continue;
            double km = MatchingEngine.distanceKm(me, req);
            pendingById.put(req.getId(), req);
            pendingModel.addElement(String.format("%s  %s%s x%d  %s, %s  ·  %.1f km  ·  %s",
                req.getId(), req.isUrgent() ? "URGENT " : "", req.getBloodGroup(),
                req.getBags(), req.getHospital(), req.getDistrict(), km,
                req.getRecipientName()));
        }
        if (pendingModel.isEmpty())
            pendingModel.addElement("No pending alerts right now.");
    }

    private void actOnSelected(boolean accept) {
        String id = pendingList.getSelectedValue();
        if (id == null) { ThemeKits.error(this, "Select an alert from the list first."); return; }
        BloodRequest req = pendingById.get(id.split("  ")[0]);
        if (req == null) return;
        if (!accept) return;
        double km = MatchingEngine.distanceKm(me, req);
        Store.setResponse(req, me, BloodRequest.Response.ACCEPTED);
        alertLog.add(0, String.format("ACCEPTED   %s  %s x%d  %s (%.1f km)",
            req.getId(), req.getBloodGroup(), req.getBags(), req.getHospital(), km));
        refreshAlerts();
        refreshStatus();
        refreshRequests();
        ThemeKits.info(this, "Thank you — the recipient can now see you on their request.");
    }
}

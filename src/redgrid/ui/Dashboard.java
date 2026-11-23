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
    private final JLabel statDonorStatus = Theme.label("", Theme.sans(Font.BOLD, 16), Theme.INK);
    private final JLabel statDonorSubtitle = Theme.label("", Theme.mono(11), Theme.MUTED);
    private final JLabel statAlertCount = Theme.label("0", Theme.display(Font.BOLD, 22), Theme.ACCENT);
    private final JLabel statReqCount = Theme.label("0", Theme.display(Font.BOLD, 22), Theme.INK);
    private final JLabel statReqSubtitle = Theme.label("No active requests", Theme.mono(11), Theme.MUTED);

    private final DefaultListModel<String> myRequests = new DefaultListModel<>();
    private final DefaultListModel<String> alertLog = new DefaultListModel<>();
    private final DefaultListModel<String> pendingModel = new DefaultListModel<>();
    private final java.util.Map<String, BloodRequest> pendingById = new java.util.LinkedHashMap<>();
    private final JList<String> pendingList = new JList<>(pendingModel);
    private final JList<String> myReqList = new JList<>(myRequests);
    private final JList<String> historyList = new JList<>(alertLog);

    public Dashboard(Donor me) {
        super("RedGrid — Dashboard");
        this.me = me;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1020, 700);
        setLocationRelativeTo(null);

        JComponent tabs = TabStrip.build(
            new String[]{"Overview", "Blood Request", "Profile"},
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

    private JPanel createMetricCard(String kicker, JComponent primary, JComponent secondary) {
        JPanel card = Theme.panel(Theme.SURFACE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LINE, 1),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.add(Theme.kicker(kicker));
        card.add(Box.createVerticalStrut(10));
        primary.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(primary);
        card.add(Box.createVerticalStrut(4));
        secondary.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(secondary);
        return card;
    }

    private JPanel overviewTab() {
        JPanel body = Theme.panel(Theme.BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(24, 48, 32, 48));

        // 1. Top Metric Cards (3 columns)
        JPanel statsGrid = Theme.panel(Theme.BG);
        statsGrid.setLayout(new GridLayout(1, 3, 16, 0));
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        JPanel donorCard = createMetricCard("DONOR PROFILE", statDonorStatus, statDonorSubtitle);
        JPanel alertCountCard = createMetricCard("ACTIVE ALERTS NEAR YOU", statAlertCount,
                Theme.label("Within 25 km compatible radius", Theme.mono(11), Theme.MUTED));
        JPanel reqCountCard = createMetricCard("MY BLOOD REQUESTS", statReqCount, statReqSubtitle);

        statsGrid.add(donorCard);
        statsGrid.add(alertCountCard);
        statsGrid.add(reqCountCard);

        body.add(statsGrid);
        body.add(Box.createVerticalStrut(20));

        // 2. Urgent Live Alerts Section
        JPanel alertsCard = ThemeKits.card("EMERGENCY REQUESTS NEAR YOU");
        alertsCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        pendingList.setFont(Theme.mono(12));
        pendingList.setBackground(Theme.SURFACE);
        pendingList.setForeground(Theme.INK);
        pendingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollAlerts = new JScrollPane(pendingList);
        scrollAlerts.setPreferredSize(new Dimension(760, 115));
        scrollAlerts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        scrollAlerts.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        forwardMouseWheel(scrollAlerts);

        JPanel alertActionRow = Theme.panel(Theme.SURFACE);
        alertActionRow.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
        alertActionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton acceptBtn = Theme.button("Accept Selected Request", true);
        acceptBtn.setPreferredSize(new Dimension(200, 38));
        acceptBtn.addActionListener(e -> actOnSelected(true));
        alertActionRow.add(acceptBtn);
        JLabel alertHelp = Theme.label("Select a request above to respond as a donor.", Theme.sans(Font.PLAIN, 12), Theme.MUTED);
        alertActionRow.add(alertHelp);

        alertsCard.add(scrollAlerts);
        alertsCard.add(Box.createVerticalStrut(12));
        alertsCard.add(alertActionRow);

        body.add(alertsCard);
        body.add(Box.createVerticalStrut(20));

        // 3. Two Column Section (My Posted Requests + Activity / Cooldown)
        JPanel twoCol = Theme.panel(Theme.BG);
        twoCol.setLayout(new GridLayout(1, 2, 16, 0));
        twoCol.setAlignmentX(Component.LEFT_ALIGNMENT);
        twoCol.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        // Left Col: My Requests
        JPanel myReqCard = ThemeKits.card("MY POSTED REQUESTS");
        myReqList.setFont(Theme.mono(12));
        myReqList.setBackground(Theme.SURFACE);
        myReqList.setForeground(Theme.INK);
        JScrollPane myReqScroll = new JScrollPane(myReqList);
        myReqScroll.setPreferredSize(new Dimension(360, 140));
        myReqScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        forwardMouseWheel(myReqScroll);
        myReqCard.add(myReqScroll);

        // Right Col: Activity / Cooldown History
        JPanel historyCard = ThemeKits.card("ACTIVITY & DONATION LOG");
        historyList.setFont(Theme.mono(12));
        historyList.setBackground(Theme.SURFACE);
        historyList.setForeground(Theme.INK);
        JScrollPane histScroll = new JScrollPane(historyList);
        histScroll.setPreferredSize(new Dimension(360, 140));
        histScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        forwardMouseWheel(histScroll);
        historyCard.add(histScroll);

        twoCol.add(myReqCard);
        twoCol.add(historyCard);

        body.add(twoCol);
        body.add(Box.createVerticalStrut(20));

        return wrapTab(body);
    }

    private void forwardMouseWheel(JScrollPane childScroll) {
        childScroll.addMouseWheelListener(e -> {
            JScrollPane parent = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, childScroll);
            if (parent != null) {
                // If child cannot scroll further in that direction, dispatch to parent scrollpane
                JScrollBar bar = childScroll.getVerticalScrollBar();
                int val = bar.getValue();
                int min = bar.getMinimum();
                int max = bar.getMaximum() - bar.getVisibleAmount();
                if ((e.getWheelRotation() < 0 && val <= min) || (e.getWheelRotation() > 0 && val >= max)) {
                    parent.dispatchEvent(SwingUtilities.convertMouseEvent(childScroll, e, parent));
                }
            }
        });
    }

    private void refreshStatus() {
        if (!me.isAvailableAsDonor()) {
            statDonorStatus.setText(me.getBloodGroup().label() + " · Alerts Disabled");
            statDonorStatus.setForeground(Theme.DIM);
            statDonorSubtitle.setText("Enable 'Available as Donor' in Profile tab");
            return;
        }

        if (me.getLocation() == null) {
            statDonorStatus.setText(me.getBloodGroup().label() + " · Location Needed");
            statDonorStatus.setForeground(Theme.ACCENT);
            statDonorSubtitle.setText("Set your district/upazila in Profile tab");
            return;
        }

        if (me.isEligible()) {
            statDonorStatus.setText(me.getBloodGroup().label() + " · Eligible to Donate");
            statDonorStatus.setForeground(Theme.SUCCESS);
            statDonorSubtitle.setText(me.getLastDonation() == null
                ? "Ready for emergency calls"
                : "Last donated " + FMT.format(me.getLastDonation()));
        } else {
            statDonorStatus.setText(me.getBloodGroup().label() + " · Resting (" + me.daysUntilEligible() + "d left)");
            statDonorStatus.setForeground(Theme.ACCENT);
            statDonorSubtitle.setText("90-day recovery active · " + me.getDistrict());
        }
    }

    private void refreshRequests() {
        myRequests.clear();
        int activeCount = 0;
        for (BloodRequest r : Store.getRequests()) {
            if (!r.getRecipientId().equals(me.getId())) continue;
            activeCount++;
            String flag = r.isUrgent() ? "URGENT" : "NORMAL";
            myRequests.addElement(String.format("%s [%s] %s x%d  %s | %s (%d/%d)",
                r.getId(), flag, r.getBloodGroup(), r.getBags(),
                r.getHospital(), r.getStatus(), r.acceptedCount(), r.getBags()));
        }
        if (myRequests.isEmpty()) {
            myRequests.addElement("No requests posted by you.");
            statReqCount.setText("0");
            statReqSubtitle.setText("All requests fulfilled or none posted");
        } else {
            statReqCount.setText(String.valueOf(activeCount));
            statReqSubtitle.setText(activeCount + " request(s) on record");
        }
    }

    // ---------- request tab ----------

    private JPanel requestTab() {
        String[] labels = new String[]{"Select Blood Group", "O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"};
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
        JPanel container = Theme.panel(Theme.BG);
        CardLayout cardLayout = new CardLayout();
        container.setLayout(cardLayout);

        // --- PREVIEW MODE PANEL ---
        JPanel previewCard = ThemeKits.card("User Profile");

        // Identity Badge + Basic info row (BorderLayout keeps height tight in BoxLayout card)
        JPanel idRow = Theme.panel(Theme.SURFACE);
        idRow.setLayout(new BorderLayout(16, 0));
        idRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        idRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JLabel bloodBadge = Theme.label(me.getBloodGroup().label(), Theme.display(Font.BOLD, 32), Theme.ACCENT);
        idRow.add(bloodBadge, BorderLayout.WEST);

        JPanel nameBox = Theme.panel(Theme.SURFACE);
        nameBox.setLayout(new BoxLayout(nameBox, BoxLayout.Y_AXIS));
        nameBox.setAlignmentY(Component.CENTER_ALIGNMENT);
        JLabel nameLabel = Theme.label(me.getName(), Theme.sans(Font.BOLD, 17), Theme.INK);
        JLabel metaLabel = Theme.label("ID: " + me.getId() + "  ·  " + me.getRole(), Theme.mono(12), Theme.DIM);
        nameBox.add(nameLabel);
        nameBox.add(Box.createVerticalStrut(2));
        nameBox.add(metaLabel);
        idRow.add(nameBox, BorderLayout.CENTER);

        previewCard.add(idRow);
        previewCard.add(Box.createVerticalStrut(20));

        // Details list
        JLabel pName = Theme.label("Full Name:  " + me.getName(), Theme.sans(Font.PLAIN, 14), Theme.INK);
        pName.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pPhone = Theme.label("Phone Number:  " + me.getPhone(), Theme.sans(Font.PLAIN, 14), Theme.INK);
        pPhone.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pBlood = Theme.label("Blood Group:  " + me.getBloodGroup().label(), Theme.sans(Font.PLAIN, 14), Theme.INK);
        pBlood.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pAddress = Theme.label(
            "Registered Location:  " + (me.getFullAddress() != null ? me.getFullAddress().toString() : "No location set yet"),
            Theme.sans(Font.PLAIN, 14), Theme.INK
        );
        pAddress.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pStatus = Theme.label(
            "Donation Status:  " + (me.isEligible() ? "Eligible to Donate" : ("Cooldown (" + me.daysUntilEligible() + " days left)")),
            Theme.sans(Font.BOLD, 14),
            me.isEligible() ? Theme.SUCCESS : Theme.ACCENT
        );
        pStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Show whether donor alerts are active
        String donorStatusText = me.isAvailableAsDonor()
            ? "Donor Alerts:  Active — you will receive emergency alerts"
            : "Donor Alerts:  Off — enable in Edit Profile to receive alerts";
        JLabel pDonorStatus = Theme.label(donorStatusText, Theme.sans(Font.PLAIN, 13),
            me.isAvailableAsDonor() ? Theme.SUCCESS : Theme.DIM);
        pDonorStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        previewCard.add(pName);
        previewCard.add(Box.createVerticalStrut(10));
        previewCard.add(pPhone);
        previewCard.add(Box.createVerticalStrut(10));
        previewCard.add(pBlood);
        previewCard.add(Box.createVerticalStrut(10));
        previewCard.add(pAddress);
        previewCard.add(Box.createVerticalStrut(10));
        previewCard.add(pStatus);
        previewCard.add(Box.createVerticalStrut(6));
        previewCard.add(pDonorStatus);
        previewCard.add(Box.createVerticalStrut(22));

        JButton editBtn = Theme.button("Edit Profile", true);
        editBtn.setPreferredSize(new Dimension(140, 42));
        editBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewCard.add(editBtn);

        JPanel previewBody = Theme.panel(Theme.BG);
        previewBody.setLayout(new BoxLayout(previewBody, BoxLayout.Y_AXIS));
        previewBody.setBorder(BorderFactory.createEmptyBorder(28, 56, 36, 56));
        previewBody.add(previewCard);
        previewBody.add(Box.createVerticalGlue());

        // --- EDIT MODE PANEL ---
        JPanel editCard = ThemeKits.card("Edit Profile");
        editCard.add(Theme.label("Update your personal details and location.", Theme.sans(Font.PLAIN, 13), Theme.MUTED));
        editCard.add(Box.createVerticalStrut(16));

        JTextField nameField = Theme.textField();
        nameField.setText(me.getName());

        JTextField phoneField = Theme.textField();
        phoneField.setText(me.getPhone());

        String[] groups = new String[]{"O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"};
        JComboBox<String> bloodCombo = Theme.comboBox(groups);
        bloodCombo.setSelectedItem(me.getBloodGroup().label());

        ThemeKits.formRow(editCard, "Full Name *", nameField);
        ThemeKits.formRow(editCard, "Phone Number *", phoneField);
        ThemeKits.formRow(editCard, "Blood Group *", bloodCombo);

        // Donor opt-in checkbox
        JCheckBox donorToggle = new JCheckBox("Available as Donor — receive emergency blood requests near me");
        donorToggle.setFont(Theme.sans(Font.BOLD, 13));
        donorToggle.setBackground(Theme.SURFACE);
        donorToggle.setForeground(Theme.INK);
        donorToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        donorToggle.setSelected(me.isAvailableAsDonor());
        editCard.add(Box.createVerticalStrut(8));
        editCard.add(donorToggle);

        editCard.add(Box.createVerticalStrut(6));
        AddressSelectorPanel addressSelector = new AddressSelectorPanel(false);
        editCard.add(addressSelector);
        editCard.add(Box.createVerticalStrut(20));

        JButton saveBtn = Theme.button("Save Changes", true);
        saveBtn.setPreferredSize(new Dimension(150, 42));

        JButton cancelBtn = Theme.button("Cancel", false);
        cancelBtn.setPreferredSize(new Dimension(120, 42));

        JPanel btnRow = Theme.panel(Theme.SURFACE);
        btnRow.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.add(saveBtn);
        btnRow.add(cancelBtn);
        editCard.add(btnRow);

        JPanel editBody = Theme.panel(Theme.BG);
        editBody.setLayout(new BoxLayout(editBody, BoxLayout.Y_AXIS));
        editBody.setBorder(BorderFactory.createEmptyBorder(28, 56, 36, 56));
        editBody.add(editCard);
        editBody.add(Box.createVerticalGlue());

        // Switch to Edit Mode
        editBtn.addActionListener(e -> {
            nameField.setText(me.getName());
            phoneField.setText(me.getPhone());
            bloodCombo.setSelectedItem(me.getBloodGroup().label());
            donorToggle.setSelected(me.isAvailableAsDonor());
            cardLayout.show(container, "edit");
        });

        // Cancel
        cancelBtn.addActionListener(e -> cardLayout.show(container, "preview"));

        // Save
        saveBtn.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newPhone = phoneField.getText().trim();
            if (newName.isEmpty()) {
                ThemeKits.error(this, "Name cannot be empty.");
                return;
            }
            if (!newPhone.matches("^01[3-9]\\d{8}$")) {
                ThemeKits.error(this, "Phone must be a valid 11-digit Bangladeshi number (e.g. 01712345678).");
                return;
            }

            BloodGroup newBg = BloodGroup.fromLabel((String) bloodCombo.getSelectedItem());
            me.setName(newName);
            me.setPhone(newPhone);
            me.setBloodGroup(newBg);
            me.setAvailableAsDonor(donorToggle.isSelected());

            redgrid.model.location.Address addr = addressSelector.getSelectedAddress();
            if (addr != null && !addr.getDistrict().isEmpty()) {
                me.setDistrict(addr.getDistrict());
                me.setFullAddress(addr);
                redgrid.model.location.District d = addressSelector.getSelectedDistrict();
                if (d != null && d.getLat() != 0) {
                    me.setLocation(new GeoPoint(d.getLat(), d.getLon()));
                }
            }

            Store.saveUsers();
            refreshStatus();

            // Update preview labels
            nameLabel.setText(me.getName());
            metaLabel.setText("ID: " + me.getId() + "  ·  " + me.getRole());
            bloodBadge.setText(me.getBloodGroup().label());
            pName.setText("Full Name:  " + me.getName());
            pPhone.setText("Phone Number:  " + me.getPhone());
            pBlood.setText("Blood Group:  " + me.getBloodGroup().label());
            pAddress.setText("Registered Location:  " + (me.getFullAddress() != null ? me.getFullAddress().toString() : "No location set yet"));
            pDonorStatus.setText(me.isAvailableAsDonor()
                ? "Donor Alerts:  Active — you will receive emergency alerts"
                : "Donor Alerts:  Off — enable in Edit Profile to receive alerts");
            pDonorStatus.setForeground(me.isAvailableAsDonor() ? Theme.SUCCESS : Theme.DIM);

            ThemeKits.info(this, "Profile updated successfully!");
            cardLayout.show(container, "preview");
        });

        container.add(previewBody, "preview");
        container.add(editBody, "edit");
        cardLayout.show(container, "preview");

        return wrapTab(container);
    }

    private JPanel wrapTab(JComponent inner) {
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
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
        int alertCount = 0;
        for (BloodRequest req : Store.getRequests()) {
            if (req.getStatus() != BloodRequest.Status.OPEN) continue;
            if (req.responseOf(me.getId()) != BloodRequest.Response.PENDING) continue;
            if (!MatchingEngine.evaluateABO_Rh(me.getBloodGroup(), req.getBloodGroup())) continue;
            if (me.getLocation() == null || req.getLocation() == null) continue;
            double km = MatchingEngine.distanceKm(me, req);
            pendingById.put(req.getId(), req);
            alertCount++;
            pendingModel.addElement(String.format("%s  %s%s x%d  %s, %s  ·  %.1f km  ·  %s",
                req.getId(), req.isUrgent() ? "URGENT " : "", req.getBloodGroup(),
                req.getBags(), req.getHospital(), req.getDistrict(), km,
                req.getRecipientName()));
        }
        statAlertCount.setText(String.valueOf(alertCount));
        if (pendingModel.isEmpty())
            pendingModel.addElement("No emergency alerts near you right now.");
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

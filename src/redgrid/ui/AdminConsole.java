package redgrid.ui;

import redgrid.model.*;
import redgrid.service.Store;

import javax.swing.*;
import java.awt.*;

/**
 * Admin console: registry audit (users), request monitor, and donation log
 * in deck-styled tables with mono headers.
 */
public class AdminConsole extends JFrame {

    public AdminConsole(Admin me) {
        super("RedGrid — Admin Console");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        JPanel root = Theme.panel(Theme.BG);
        root.setLayout(new BorderLayout());
        root.add(ThemeKits.headerBar("Admin console", me.getName(), me.getId(),
                () -> { dispose(); new LoginFrame(); }), BorderLayout.NORTH);

        JComponent tabs = TabStrip.build(
            new String[]{"User registry", "Emergency requests", "Donation log"},
            new JComponent[]{registryAudit(), requestMonitor(), donationLog()});

        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        setVisible(true);
    }

    private JPanel registryAudit() {
        String[] cols = {"ID", "Name", "Phone", "District", "Role", "Blood group", "Status"};
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (User u : Store.getUsers()) {
            String group = "—", status = "—";
            if (u instanceof Donor d) {
                group = d.getBloodGroup().label();
                status = d.eligibilityText();
            } else if (u instanceof Recipient r) {
                group = r.getBloodGroup().label();
            }
            rows.add(new String[]{u.getId(), u.getName(), u.getPhone(),
                    u.getDistrict(), u.getRole().name(), group, status});
        }
        return wrap(cols, rows.toArray(new String[0][]));
    }

    private JPanel requestMonitor() {
        String[] cols = {"ID", "Group", "Bags", "Hospital", "District", "Urgent",
                         "Accepted", "Alerted", "Status"};
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (BloodRequest r : Store.getRequests()) {
            rows.add(new String[]{r.getId(), r.getBloodGroup().label(),
                    String.valueOf(r.getBags()), r.getHospital(), r.getDistrict(),
                    r.isUrgent() ? "YES" : "no",
                    r.acceptedCount() + "/" + r.getBags(),
                    String.valueOf(r.getResponses().size()),
                    r.getStatus().name()});
        }
        return wrap(cols, rows.toArray(new String[0][]));
    }

    private JPanel donationLog() {
        String[] cols = {"ID", "Donor", "Recipient", "Group", "Hospital", "District", "Date"};
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (DonationRecord d : Store.getRecords()) {
            rows.add(new String[]{d.getId(), d.getDonorName(), d.getRecipientName(),
                    d.getBloodGroup().label(), d.getHospital(), d.getDistrict(),
                    d.getDate().toString()});
        }
        return wrap(cols, rows.toArray(new String[0][]));
    }

    private JPanel wrap(String[] cols, String[][] rows) {
        JPanel panel = Theme.panel(Theme.BG);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        JTable t = ThemeKits.table(cols, rows);
        JScrollPane scroll = new JScrollPane(t);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
}

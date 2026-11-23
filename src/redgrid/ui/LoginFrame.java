package redgrid.ui;

import redgrid.model.*;

import redgrid.service.Store;

import javax.swing.*;
import java.awt.*;

/**
 * Entry window: deck-style hero on the left ("Red" in ink, "Grid" in accent)
 * and a login card on the right. Registering an account logs straight in.
 */
public class LoginFrame extends JFrame {

    private static final String[] GROUPS = {"O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"};

    public LoginFrame() {
        super("RedGrid — Real-Time Blood Donor Network");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);

        setContentPane(buildHero());
        setVisible(true);
    }

    private JPanel buildHero() {
        JPanel root = Theme.panel(Theme.BG);
        root.setLayout(new BorderLayout());

        // left — display type
        JPanel left = Theme.panel(Theme.BG);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(0, 72, 120, 30));

        JLabel kicker = Theme.kicker("Real-Time Blood Donor Network");
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel title = new JPanel(false);
        title.setBackground(Theme.BG);
        title.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel red = Theme.label("Red", Theme.display(Font.PLAIN, 92), Theme.ACCENT);
        JLabel grid = Theme.label("Grid", Theme.display(Font.BOLD, 92), Theme.INK);
        title.add(red);
        title.add(grid);

        JTextArea sub = new JTextArea(
            "Bridge the gap between emergency recipients and\nvoluntary donors — instant matching, real-time alerts,\nand systematic 90-day donor cooldown tracking.");
        sub.setEditable(false);
        sub.setLineWrap(false);
        sub.setFont(Theme.sans(Font.PLAIN, 16));
        sub.setForeground(Theme.MUTED);
        sub.setBackground(Theme.BG);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(Box.createVerticalGlue());
        left.add(kicker);
        left.add(Box.createVerticalStrut(18));
        left.add(title);
        left.add(Box.createVerticalStrut(24));
        left.add(sub);
        left.add(Box.createVerticalGlue());

        // right — login card
        JPanel right = Theme.panel(Theme.BG);
        right.setLayout(new GridBagLayout());
        right.setBorder(BorderFactory.createEmptyBorder(40, 30, 40, 72));
        right.add(loginCard());

        root.add(left, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);
        return root;
    }

    private JPanel loginCard() {
        JTextField idField = Theme.textField();
        JPasswordField passField = Theme.passwordField();
        JButton login = Theme.button("Log in", true);
        JButton register = Theme.button("Register", false);

        login.addActionListener(e -> {
            User u = Store.verifyCredentials(idField.getText().trim(), new String(passField.getPassword()));
            if (u == null) {
                ThemeKits.error(this, "Invalid ID/phone or password.");
                return;
            }
            dispose();
            openConsole(u);
        });

        register.addActionListener(e -> showRegisterDialog());

        JPanel card = Theme.panel(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LINE, 1),
            BorderFactory.createEmptyBorder(32, 32, 32, 32)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.add(Box.createVerticalGlue());
        card.add(Theme.kicker("Authentication"));
        card.add(Box.createVerticalStrut(14));
        card.add(Theme.label("Welcome back", Theme.display(Font.BOLD, 26), Theme.INK));
        card.add(Box.createVerticalStrut(24));

        ThemeKits.formRow(card, "ID / Phone", idField);
        ThemeKits.formRow(card, "Password", passField);

        card.add(Box.createVerticalStrut(10));
        card.add(ThemeKits.row(login, register));
        card.add(Box.createVerticalGlue());
        return card;
    }

    /** Minimal sign-up: name, phone, password, blood group — then straight in. */
    private void showRegisterDialog() {
        JDialog d = new JDialog(this, "Register — RedGrid", true);
        d.setSize(430, 470);
        d.setLocationRelativeTo(this);

        JTextField name = Theme.textField();
        JTextField phone = Theme.textField();
        JPasswordField pass = Theme.passwordField();
        JComboBox<String> group = Theme.comboBox(GROUPS);
        JButton create = Theme.button("Create account", true);
        JButton cancel = Theme.button("Cancel", false);

        create.addActionListener(e -> {
            BloodGroup bg = BloodGroup.fromLabel((String) group.getSelectedItem());
            String err = Store.registerUser(name.getText(), phone.getText().trim(),
                    new String(pass.getPassword()), bg);
            if (err != null) { ThemeKits.error(d, err); return; }
            User u = Store.verifyCredentials(phone.getText().trim(),
                    new String(pass.getPassword()));
            d.dispose();
            dispose();
            openConsole(u);
        });
        cancel.addActionListener(e -> d.dispose());

        JPanel body = Theme.panel(Theme.SURFACE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        body.add(Theme.kicker("Registration"));
        body.add(Box.createVerticalStrut(16));
        ThemeKits.formRow(body, "Full name", name);
        ThemeKits.formRow(body, "Phone (01XXXXXXXXX)", phone);
        ThemeKits.formRow(body, "Password", pass);
        ThemeKits.formRow(body, "Blood group", group);
        body.add(Box.createVerticalStrut(4));
        body.add(ThemeKits.row(create, cancel));

        d.setContentPane(body);
        d.setVisible(true);
    }

    private void openConsole(User u) {
        switch (u.getRole()) {
            case ADMIN -> new AdminConsole((Admin) u);
            default -> new Dashboard((Donor) u); // dashboard wires its own alert observer
        }
    }
}

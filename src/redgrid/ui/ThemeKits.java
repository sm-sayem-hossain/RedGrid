package redgrid.ui;

import javax.swing.*;
import java.awt.*;

/** Shared little helpers for the RedGrid forms and dialogs. */
final class ThemeKits {

    private ThemeKits() {}

    /** Adds one labelled form row to a vertical BoxLayout panel. */
    static void formRow(JPanel box, String labelText, JComponent field) {
        JPanel wrap = Theme.panel(box.getBackground());
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = Theme.label(labelText, Theme.mono(10), Theme.DIM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrap.add(l);
        wrap.add(Box.createVerticalStrut(5));
        wrap.add(field);
        box.add(wrap);
        box.add(Box.createVerticalStrut(14));
    }

    /** Horizontal row of components, left aligned inside a BoxLayout panel. */
    static JPanel row(JComponent... items) {
        JPanel p = Theme.panel(new Color(0, 0, 0, 0));
        p.setOpaque(false);
        p.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JComponent c : items) p.add(c);
        return p;
    }

    /** Shared top bar: kicker, role title, user meta (mono), logout. */
    static JPanel headerBar(String roleTitle, String userName, String userId, Runnable onLogout) {
        JPanel bar = Theme.panel(Theme.SURFACE);
        bar.setLayout(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
            BorderFactory.createEmptyBorder(16, 56, 16, 56)));

        JPanel left = Theme.panel(Theme.SURFACE);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(Theme.kicker("RedGrid"));
        left.add(Box.createVerticalStrut(4));
        left.add(Theme.label(roleTitle, Theme.display(Font.BOLD, 24), Theme.INK));
        bar.add(left, BorderLayout.WEST);

        JPanel right = Theme.panel(Theme.SURFACE);
        right.setLayout(new FlowLayout(FlowLayout.RIGHT, 14, 6));
        JLabel meta = Theme.label(userName + " · " + userId, Theme.mono(11), Theme.DIM);
        right.add(meta);
        JButton out = Theme.button("Log out", false);
        out.setPreferredSize(new Dimension(110, 36));
        out.addActionListener(e -> onLogout.run());
        right.add(out);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /** Deck-styled card: surface, hairline border, kicker header. */
    static JPanel card(String kickerText) {
        JPanel p = Theme.panel(Theme.SURFACE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.LINE, 1),
            BorderFactory.createEmptyBorder(24, 28, 24, 28)));
        p.add(Theme.kicker(kickerText));
        p.add(Box.createVerticalStrut(16));
        return p;
    }

    static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "RedGrid", JOptionPane.ERROR_MESSAGE);
    }

    static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "RedGrid", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Builds a styled JTable from a two-dimensional snapshot. */
    static JTable table(String[] cols, String[][] rows) {
        JTable t = new JTable(rows, cols);
        Theme.styleTable(t);
        t.setEnabled(false);
        return t;
    }
}

package redgrid.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Flat rectangular tab strip built from theme buttons — selected tab is an
 * accent-filled button, the rest are hairline outlines. No platform tab UI.
 */
public final class TabStrip {

    public record Strip(JComponent view, Runnable[] nothing) {}

    private TabStrip() {}

    /**
     * Builds a full tabbed view: a button bar on top and a card panel below.
     * Page components keep their own styling — no extra frame borders.
     */
    public static JComponent build(String[] labels, JComponent[] pages) {
        JPanel bar = Theme.panel(Theme.BG);
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 12));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
            BorderFactory.createEmptyBorder(0, 40, 12, 40)));

        CardLayout cards = new CardLayout();
        JPanel deck = Theme.panel(Theme.BG);
        deck.setLayout(cards);

        JButton[] buttons = new JButton[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            JButton b = Theme.button(labels[i], i == 0);
            b.addActionListener(e -> {
                for (int j = 0; j < buttons.length; j++) applyStyle(buttons[j], j == idx);
                cards.show(deck, "page" + idx);
            });
            buttons[i] = b;
            bar.add(b);
            deck.add(pages[i], "page" + i);
        }

        JPanel root = Theme.panel(Theme.BG);
        root.setLayout(new BorderLayout());
        root.add(bar, BorderLayout.NORTH);
        root.add(deck, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> applyStyle(buttons[0], true));
        return root;
    }

    private static void applyStyle(JButton b, boolean selected) {
        if (selected) {
            b.setBackground(Theme.ACCENT);
            b.setForeground(Theme.SURFACE);
            b.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        } else {
            b.setBackground(Theme.SURFACE);
            b.setForeground(Theme.INK);
            b.setBorder(new javax.swing.border.LineBorder(Theme.LINE_STRONG, 1, false));
        }
        b.repaint();
    }
}

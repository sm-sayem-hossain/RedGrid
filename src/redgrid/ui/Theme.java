package redgrid.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Visual design tokens lifted from the RedGrid proposal deck: warm paper
 * background, off-white surfaces, near-black ink, a single red accent and
 * hairline rules. Sharp corners, generous whitespace, mono metadata.
 */
public final class Theme {

    public static final Color BG          = new Color(0xF4EFEB);
    public static final Color BG_SOFT     = new Color(0xECE4DF);
    public static final Color SURFACE     = new Color(0xFFFDFB);
    public static final Color INK         = new Color(0x171214);
    public static final Color MUTED       = new Color(23, 18, 20, 143);
    public static final Color DIM         = new Color(23, 18, 20, 87);
    public static final Color LINE        = new Color(23, 18, 20, 33);
    public static final Color LINE_STRONG = new Color(23, 18, 20, 61);
    public static final Color ACCENT      = new Color(0xE2574C);
    public static final Color ACCENT_SOFT = new Color(226, 87, 76, 31);
    public static final Color SUCCESS     = new Color(0x2E7D4F);

    public static final Font SANS  = sans(Font.PLAIN, 14);
    public static final Font MONO  = mono(12);

    private Theme() {}

    public static Font sans(int style, int size) {
        return new Font("Helvetica Neue", style, size);
    }

    public static Font display(int style, int size) { return sans(style, size); }

    public static Font mono(int size) {
        String name = "Menlo";
        if (!isFontPresent(name)) name = Font.MONOSPACED;
        return new Font(name, Font.PLAIN, size);
    }

    private static boolean isFontPresent(String name) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (Font f : ge.getAllFonts()) if (f.getFontName().equals(name)) return true;
        return false;
    }

    // ---------- component factories ----------

    public static JPanel panel(Color bg) {
        JPanel p = new JPanel();
        p.setBackground(bg);
        p.setBorder(BorderFactory.createEmptyBorder());
        return p;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /** Small mono kicker text (deck-style section labels like "01 — AUTH"). */
    public static JLabel kicker(String text) {
        JLabel l = label(text.toUpperCase(), MONO, ACCENT);
        return l;
    }

    public static JTextField textField() {
        JTextField f = new JTextField();
        f.setFont(SANS);
        f.setForeground(INK);
        f.setBackground(SURFACE);
        f.setCaretColor(INK);
        f.setBorder(fieldBorder());
        f.setPreferredSize(new Dimension(260, 40));
        return f;
    }

    public static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(SANS);
        f.setForeground(INK);
        f.setBackground(SURFACE);
        f.setCaretColor(INK);
        f.setBorder(fieldBorder());
        f.setPreferredSize(new Dimension(260, 40));
        return f;
    }

    public static JComboBox<String> comboBox(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(SANS);
        c.setBackground(SURFACE);
        c.setForeground(INK);
        c.setBorder(fieldBorder());
        c.setPreferredSize(new Dimension(260, 40));
        return c;
    }

    private static javax.swing.border.Border fieldBorder() {
        return new CompoundBorder(new LineBorder(LINE_STRONG, 1, false),
                                  new EmptyBorder(8, 12, 8, 12));
    }

    /** Deck-style flat button. {@code filled} = accent, otherwise hairline outline. */
    public static JButton button(String text, boolean filled) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
            @Override public void updateUI() { /* keep Basic UI — no macOS Aqua */ }
        };
        // strip the platform look (rounded Aqua on macOS) so the boxy theme wins
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setOpaque(true);
        b.setFont(sans(Font.PLAIN, 14));
        b.setFocusPainted(false);
        b.setBorderPainted(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140, 42));
        if (filled) {
            b.setBackground(ACCENT);
            b.setForeground(SURFACE);
            b.setBorder(new EmptyBorder(10, 22, 10, 22));
        } else {
            b.setBackground(SURFACE);
            b.setForeground(INK);
            b.setBorder(new LineBorder(LINE_STRONG, 1, false));
        }
        return b;
    }

    public static void styleTable(JTable t) {
        t.setFont(SANS);
        t.setForeground(INK);
        t.setBackground(SURFACE);
        t.setRowHeight(34);
        t.setShowGrid(true);
        t.setGridColor(LINE);
        t.setSelectionBackground(ACCENT_SOFT);
        t.setSelectionForeground(INK);
        t.setFillsViewportHeight(true);
        JTableHeader h = t.getTableHeader();
        h.setBackground(BG_SOFT);
        h.setForeground(MUTED);
        h.setFont(mono(11));
        h.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
    }
}

package redgrid;

import redgrid.service.AlertDispatcher;
import redgrid.service.Store;
import redgrid.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * RedGrid — Real-Time Blood Donor Network and Management System.
 * Pure Java desktop application: Swing front end, OOP domain model,
 * serialized binary persistence in ./data.
 */
public final class Main {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        // boxy flat buttons everywhere — no macOS Aqua rounding, even in dialogs
        UIManager.put("ButtonUI", "javax.swing.plaf.basic.BasicButtonUI");
        UIManager.put("OptionPane.messageFont", redgrid.ui.Theme.SANS);
        UIManager.put("OptionPane.buttonFont", redgrid.ui.Theme.SANS);
        Store.saveAll(); // touch store so files/admin seed exist before UI

        SwingUtilities.invokeLater(() -> {
            UIManager.put("OptionPane.background", redgrid.ui.Theme.SURFACE);
            UIManager.put("Panel.background", redgrid.ui.Theme.SURFACE);
            new LoginFrame();
            AlertDispatcher.start();
        });
    }
}

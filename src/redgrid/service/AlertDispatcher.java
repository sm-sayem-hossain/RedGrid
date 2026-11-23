package redgrid.service;

import redgrid.model.*;

import javax.swing.SwingUtilities;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Multithreaded alert dispatcher. A daemon worker scans the request registry
 * every few seconds and pushes emergency pop-ups to matching donors through a
 * callback (the DonorHub registers itself as the observer while logged in).
 */
public final class AlertDispatcher {

    private static final long POLL_MS = 4000;

    /** donorId -> handler that shows the accept/decline dialog for a request */
    private static final Map<String, Consumer<BloodRequest>> observers = new HashMap<>();

    private static Thread worker;

    private AlertDispatcher() {}

    /** Registers the currently logged-in donor's hub as alert observer. */
    public static void notifyObserver(Donor donor, Consumer<BloodRequest> handler) {
        observers.put(donor.getId(), handler);
    }

    public static void removeObserver(Donor donor) {
        observers.remove(donor.getId());
    }

    /** Starts (once) the background broadcast task. */
    public static synchronized void start() {
        if (worker != null && worker.isAlive()) return;
        worker = new Thread(AlertDispatcher::loop, "AlertDispatcher");
        worker.setDaemon(true);
        worker.start();
    }

    private static void loop() {
        while (true) {
            try {
                Thread.sleep(POLL_MS);
                broadcastTask();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** Finds pending alerts whose donor has a registered observer and dispatches them. */
    private static void broadcastTask() {
        for (BloodRequest req : Store.getRequests()) {
            if (req.getStatus() != BloodRequest.Status.OPEN) continue;
            for (Map.Entry<String, BloodRequest.Response> e : req.getResponses().entrySet()) {
                if (e.getValue() != BloodRequest.Response.PENDING) continue;
                Consumer<BloodRequest> handler = observers.get(e.getKey());
                if (handler != null) {
                    BloodRequest r = req;
                    SwingUtilities.invokeLater(() -> handler.accept(r));
                }
            }
        }
    }
}

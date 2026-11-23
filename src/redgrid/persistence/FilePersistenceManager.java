package redgrid.persistence;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Local binary persistence using pure Java Object Serialization — no external
 * database. Every entity list is stored in its own {@code .dat} file inside
 * {@code ./data}. A missing or unreadable file simply yields an empty list so
 * the application always boots cleanly.
 */
public final class FilePersistenceManager {

    private static final File DIR = new File("data");

    static { if (!DIR.exists()) DIR.mkdirs(); }

    private FilePersistenceManager() {}

    public static <T extends Serializable> void write(String fileName, List<T> items) {
        File f = new File(DIR, fileName);
        File tmp = new File(DIR, fileName + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tmp))) {
            out.writeObject(new ArrayList<>(items));
            if (!tmp.renameTo(f)) {
                // renameTo fails on some platforms when target exists
                f.delete();
                tmp.renameTo(f);
            }
        } catch (IOException e) {
            System.err.println("[RedGrid] could not save " + fileName + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> List<T> read(String fileName) {
        File f = new File(DIR, fileName);
        if (!f.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = in.readObject();
            return (ArrayList<T>) obj;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.err.println("[RedGrid] could not load " + fileName + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

}

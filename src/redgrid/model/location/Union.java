package redgrid.model.location;

import java.io.Serializable;

/**
 * Represents a Union or Ward under an Upazila.
 */
public class Union implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String upazilaId;
    private final String name;
    private final String bnName;

    public Union(String id, String upazilaId, String name, String bnName) {
        this.id = id;
        this.upazilaId = upazilaId;
        this.name = name;
        this.bnName = bnName;
    }

    public String getId() { return id; }
    public String getUpazilaId() { return upazilaId; }
    public String getName() { return name; }
    public String getBnName() { return bnName; }

    @Override
    public String toString() {
        return name;
    }
}

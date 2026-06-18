public enum Status {
    NONE("", "—"),
    PENDING("pending", "Εκκρεμεί"),
    IN_PROGRESS("in_progress", "Σε εξέλιξη"),
    COMPLETED("completed", "Ολοκληρωμένο"),
    ABANDONED("abandoned", "Εγκαταλείφθηκε");

    private final String key;
    private final String label;

    Status(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }

    public static Status fromKey(String key) {
        if (key == null || key.isBlank()) return NONE;
        for (Status status : values()) {
            if (status.key.equalsIgnoreCase(key.trim())) return status;
        }
        return NONE;
    }

    public static Status fromLabel(String label) {
        if (label == null || label.isBlank() || label.equals("—")) return NONE;
        for (Status status : values()) {
            if (status.label.equalsIgnoreCase(label.trim())) return status;
        }
        return NONE;
    }
}

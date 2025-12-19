package org.filewatcher.filewatcher;

public enum Type {
    CREATED,
    MODIFIED,
    DELETED;

    public static Type fromEventKind(String name) {
        return switch (name) {
            case "ENTRY_CREATE" -> CREATED;
            case "ENTRY_MODIFY" -> MODIFIED;
            case "ENTRY_DELETE" -> DELETED;
            default -> throw new IllegalArgumentException("Unknown event kind: " + name);
        };
    }
}
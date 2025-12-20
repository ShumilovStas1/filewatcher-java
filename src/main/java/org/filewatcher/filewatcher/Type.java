package org.filewatcher.filewatcher;

import org.springframework.boot.devtools.filewatch.ChangedFile;

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

    public static Type fromChangedFileType(ChangedFile.Type type) {
        return switch (type) {
            case ADD -> CREATED;
            case MODIFY -> MODIFIED;
            case DELETE -> DELETED;
        };
    }
}
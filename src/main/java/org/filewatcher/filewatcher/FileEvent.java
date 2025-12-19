package org.filewatcher.filewatcher;

import java.nio.file.Path;

public record FileEvent(Type type, Path target, boolean isDir, Path rootDir) {

    @Override
    public String toString() {
        return "FileEvent{" +
               "type=" + type +
               ", target=" + target +
               ", isDir=" + isDir +
               ", rootDir=" + rootDir +
               '}';
    }
}



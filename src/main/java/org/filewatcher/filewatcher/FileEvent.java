package org.filewatcher.filewatcher;

import java.nio.file.Path;

public record FileEvent(Type type, Path target, boolean isDir, Path rootDir) {
}



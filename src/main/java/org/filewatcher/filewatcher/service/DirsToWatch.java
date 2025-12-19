package org.filewatcher.filewatcher.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class DirsToWatch {
    private static final Logger log = LoggerFactory.getLogger(DirsToWatch.class);
    private final List<Path> validDirs;

    public DirsToWatch(@Value("#{'${directories.to.watch}'.split(',')}") List<Path> directoriesToWatch) {
        Objects.requireNonNull(directoriesToWatch);
        List<Path> sortedDirs = directoriesToWatch.stream()
                .filter(Objects::nonNull)
                .map(Path::toAbsolutePath)
                .filter(this::isPathValid)
                .sorted(Path::compareTo)
                .toList();
        if (sortedDirs.isEmpty()) {
            throw new IllegalArgumentException("No directories configured to watch");
        }
        this.validDirs = reduceSubpath(sortedDirs);
        log.info("Registered directories to watch: {}", validDirs);
    }

    private List<Path> reduceSubpath(List<Path> sortedPaths) {
        List<Path> result = new ArrayList<>();
        for (Path path : sortedPaths) {
            boolean isSubpath = false;
            for (Path kept : result) {
                if (path.startsWith(kept)) {
                    isSubpath = true;
                    break;
                }
            }
            if (!isSubpath) {
                result.add(path);
            }
        }
        return result;
    }

    private boolean isPathValid(Path toWatch) {
        if (!Files.isReadable(toWatch) || !Files.isDirectory(toWatch)) {
            log.warn("Invalid directory to watch: {}", toWatch);
            return false;
        }
        return true;
    }

    public List<Path> getAbsolutePaths() {
        return validDirs;
    }

    public Path findOriginalRootPath(Path resolvedSrcPath) {
        return validDirs.stream()
                .filter(resolvedSrcPath::startsWith)
                .max(Comparator.comparingInt(Path::getNameCount))
                .orElse(null);
    }
}

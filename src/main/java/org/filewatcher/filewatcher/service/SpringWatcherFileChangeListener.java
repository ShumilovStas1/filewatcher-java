package org.filewatcher.filewatcher.service;

import org.filewatcher.filewatcher.FileEvent;
import org.filewatcher.filewatcher.Type;
import org.filewatcher.filewatcher.ws.FileEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.filewatch.FileChangeListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

@Component
@ConditionalOnProperty(value = "fw.watcher-type", havingValue = "spring")
public class SpringWatcherFileChangeListener implements FileChangeListener {
    private static final Logger log = LoggerFactory.getLogger(SpringWatcherFileChangeListener.class);

    private final FileEventListener listener;

    public SpringWatcherFileChangeListener(FileEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void onChange(Set<ChangedFiles> changeSet) {
        for (ChangedFiles changedFiles : changeSet) {
            for (ChangedFile file : changedFiles) {
                log.debug("Detected file change: {}", file);
                listener.onFileEvent(convert(file));
            }
        }
    }

    private FileEvent convert(ChangedFile file) {
        String relativeName = file.getRelativeName();
        return new FileEvent(
                Type.fromChangedFileType(file.getType()),
                Path.of(relativeName),
                file.getFile().isDirectory(),
                Path.of(getRootDir(file.getFile(), relativeName))
        );
    }

    private String getRootDir(File file, String relativeName) {
        String fileName = StringUtils.cleanPath(file.getPath());
        return fileName.replace(relativeName, "");
    }
}

package org.filewatcher.filewatcher.service;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.filewatcher.filewatcher.FileEvent;
import org.filewatcher.filewatcher.Type;
import org.filewatcher.filewatcher.ws.FileEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(value = "fw.watcher-type", havingValue = "apache_commons")
public class ApacheCommonsWatcherListener implements FileAlterationListener {
    private static final Logger log = LoggerFactory.getLogger(ApacheCommonsWatcherListener.class);

    private final FileEventListener listener;
    private FileAlterationObserver observer;

    public ApacheCommonsWatcherListener(FileEventListener listener) {
        this.listener = listener;
    }

    private void changeDetected(Type type, boolean isDir, File target) {
        if (observer != null) {
            log.debug("Detected {} on {}", type, target);
            File rootDir = observer.getDirectory();
            Path relativeTargetPath = rootDir.toPath().relativize(target.toPath());
            FileEvent event = new FileEvent(
                    type,
                    relativeTargetPath,
                    isDir,
                    rootDir.toPath()
            );
            listener.onFileEvent(event);
        }
    }

    @Override
    public void onDirectoryChange(File directory) {
       //ignore to prevent events on dir content changes
    }

    @Override
    public void onDirectoryCreate(File directory) {
        changeDetected(Type.CREATED, true, directory);
    }

    @Override
    public void onDirectoryDelete(File directory) {
        changeDetected(Type.DELETED, true, directory);
    }

    @Override
    public void onFileChange(File file) {
        changeDetected(Type.MODIFIED, false, file);
    }

    @Override
    public void onFileCreate(File file) {
        changeDetected(Type.CREATED, false, file);
    }

    @Override
    public void onFileDelete(File file) {
        changeDetected(Type.DELETED, false, file);
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
        //log.info("Started monitoring with observer: {}", observer.getDirectory());
        this.observer = observer;
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
        this.observer = null;
    }
}

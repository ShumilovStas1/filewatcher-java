package org.filewatcher.filewatcher.ws;

import org.filewatcher.filewatcher.FileEvent;

public interface FileEventListener {
    void onFileEvent(FileEvent event);
}

package org.filewatcher.filewatcher.service;

import org.filewatcher.filewatcher.FileEvent;
import org.filewatcher.filewatcher.ws.FileEventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class CollectingFileEventListener implements FileEventListener {
    List<FileEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void onFileEvent(FileEvent event) {
        events.add(event);
    }

    public List<FileEvent> getEvents() {
        return events;
    }

    public List<FileEvent> awaitEvents(int expectedCount) {
        int attempts = 0;
        List<FileEvent> events = getEvents();
        while (events.size() < expectedCount && attempts < 10) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            events = getEvents();
            attempts++;
        }
        if (events.size() < expectedCount) {
            throw new AssertionError("Expected " + expectedCount + " events but got " + events.size());
        }
        return events;
    }

}

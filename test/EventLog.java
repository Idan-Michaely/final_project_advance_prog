package test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Thread-safe singleton that records the last {@value #MAX_ENTRIES} publish events.
 * Each entry is a human-readable string: {@code "HH:mm:ss  TopicName → value"}.
 */
public class EventLog {

    private static final int MAX_ENTRIES = 30;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final EventLog INSTANCE = new EventLog();

    private final LinkedList<String> entries = new LinkedList<>();

    private EventLog() {}

    public static EventLog get() { return INSTANCE; }

    /** Adds a new event to the front of the log and trims to {@value #MAX_ENTRIES}. */
    public synchronized void add(String topicName, String value) {
        String ts = LocalTime.now().format(FMT);
        entries.addFirst(ts + "  " + topicName + " → " + value);
        while (entries.size() > MAX_ENTRIES) entries.removeLast();
    }

    /** Returns a snapshot of all current log entries (newest first). */
    public synchronized List<String> getAll() {
        return new ArrayList<>(entries);
    }
}

package test;

import java.util.ArrayList;
import java.util.List;

/**
 * A named channel in the pub/sub system.
 * Agents subscribe to receive messages and publish through a Topic.
 * Instances are managed exclusively by {@link TopicManagerSingleton.TopicManager}.
 *
 * <p>The {@code subs} and {@code pubs} lists are package-private for
 * {@link Graph#createFromTopics()} access. Public getters are also provided
 * for use by the web layer (servlets and graph writer).
 */
public class Topic {

    /** The unique name of this topic. */
    public final String name;

    /** Agents currently subscribed to this topic. */
    List<Agent> subs;

    /** Agents registered as publishers of this topic. */
    List<Agent> pubs;

    /** Most recent published value, as text. Empty string until first publish. */
    private String result = "";

    Topic(String name) {
        this.name = name;
        this.subs = new ArrayList<>();
        this.pubs = new ArrayList<>();
    }

    public void subscribe(Agent a) {
        if (a != null && !subs.contains(a)) subs.add(a);
    }

    public void unsubscribe(Agent a) {
        subs.remove(a);
    }

    /**
     * Publishes a message to all current subscribers and records the value.
     *
     * @param m the message to publish; ignored if null
     */
    public void publish(Message m) {
        if (m == null) return;
        result = m.asText;
        List<Agent> snapshot = new ArrayList<>(subs);
        for (Agent a : snapshot) {
            a.callback(this.name, m);
        }
    }

    public void addPublisher(Agent a) {
        if (a != null && !pubs.contains(a)) pubs.add(a);
    }

    public void removePublisher(Agent a) {
        pubs.remove(a);
    }

    /** Returns the text of the most recently published message, or "" if none. */
    public String getResult() { return result; }

    /** Returns a snapshot of the current subscriber list. */
    public List<Agent> getSubscribers() { return new ArrayList<>(subs); }

    /** Returns a snapshot of the current publisher list. */
    public List<Agent> getPublishers() { return new ArrayList<>(pubs); }
}

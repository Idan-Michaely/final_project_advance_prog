package test;

/**
 * Computes the arithmetic average of two input topics and publishes the result.
 * Waits until both inputs have been received before publishing.
 */
public class AverageAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;

    private Double x = null;
    private Double y = null;

    public AverageAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
    }

    @Override public String getName() { return "AverageAgent"; }

    @Override
    public void reset() {
        x = null;
        y = null;
    }

    @Override
    public void callback(String topic, Message msg) {
        if (Double.isNaN(msg.asDouble)) return;
        if (subs.length > 0 && topic.equals(subs[0]))      x = msg.asDouble;
        else if (subs.length > 1 && topic.equals(subs[1])) y = msg.asDouble;

        if (x != null && y != null && pubs.length > 0) {
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message((x + y) / 2.0));
        }
    }

    @Override public void close() {}
}

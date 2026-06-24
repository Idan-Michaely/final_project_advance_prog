package test;

/**
 * Multiplies two input values and publishes the product.
 * Waits until both inputs have been received before publishing.
 */
public class MultiplyAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;

    private Double x = null;
    private Double y = null;

    public MultiplyAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
    }

    @Override public String getName() { return "MultiplyAgent"; }

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
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(x * y));
        }
    }

    @Override public void close() {}
}

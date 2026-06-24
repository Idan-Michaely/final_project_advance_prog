package test;

/**
 * An agent that adds the values from two input topics and publishes the sum
 * to an output topic.
 *
 * <p>The constructor <strong>does not</strong> subscribe to topics; subscription is
 * performed by {@link GenericConfig}, which wraps this agent in a
 * {@link ParallelAgent} and registers the wrapper as the actual subscriber.
 * This ensures messages are processed asynchronously.
 *
 * <p>The first message on {@code subs[0]} sets {@code x}; the first message on
 * {@code subs[1]} sets {@code y}. The sum is published only after both have been
 * received at least once.
 */
public class PlusAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;

    /** Latest value from the first input; {@code null} until first message. */
    private Double x = null;

    /** Latest value from the second input; {@code null} until first message. */
    private Double y = null;

    /**
     * Creates a PlusAgent with the given topic configuration.
     * Topics are subscribed externally by {@link GenericConfig}.
     *
     * @param subs input topic names (subs[0] = x, subs[1] = y)
     * @param pubs output topic names (pubs[0] receives x+y)
     */
    public PlusAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
    }

    @Override
    public String getName() { return "PlusAgent"; }

    @Override
    public void reset() {
        x = null;
        y = null;
    }

    /**
     * Updates the stored value for the incoming topic, then publishes
     * {@code x + y} to {@code pubs[0]} if both inputs have been received.
     *
     * @param topic the topic that triggered this callback
     * @param msg   the incoming message
     */
    @Override
    public void callback(String topic, Message msg) {
        if (subs.length > 0 && topic.equals(subs[0]))      x = msg.asDouble;
        else if (subs.length > 1 && topic.equals(subs[1])) y = msg.asDouble;

        if (x != null && y != null && pubs.length > 0) {
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(x + y));
        }
    }

    @Override
    public void close() {}
}

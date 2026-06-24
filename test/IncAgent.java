package test;

/**
 * An agent that increments a numeric value by 1 and publishes the result.
 *
 * <p>The constructor <strong>does not</strong> subscribe to topics; subscription is
 * performed by {@link GenericConfig}, which wraps this agent in a
 * {@link ParallelAgent} and registers the wrapper as the actual subscriber.
 *
 * <p>Non-numeric messages (where {@link Message#asDouble} is {@link Double#NaN})
 * are silently ignored.
 */
public class IncAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;

    /**
     * Creates an IncAgent with the given topic configuration.
     * Topics are subscribed externally by {@link GenericConfig}.
     *
     * @param subs input topic names (subs[0] is the input)
     * @param pubs output topic names (pubs[0] receives value+1)
     */
    public IncAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
    }

    @Override
    public String getName() { return "IncAgent"; }

    @Override
    public void reset() {}

    /**
     * Publishes {@code msg.asDouble + 1} to {@code pubs[0]}.
     * Ignored if the message value is NaN or there is no output topic.
     *
     * @param topic the topic that triggered this callback
     * @param msg   the incoming message
     */
    @Override
    public void callback(String topic, Message msg) {
        if (!Double.isNaN(msg.asDouble) && pubs.length > 0) {
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(msg.asDouble + 1));
        }
    }

    @Override
    public void close() {}
}

package test;

import java.util.function.BinaryOperator;

import test.TopicManagerSingleton.TopicManager;

/**
 * An agent that subscribes to two input topics, applies a binary operation
 * to their latest values, and publishes the result to an output topic.
 *
 * <p>The operation is supplied as a {@link BinaryOperator}&lt;{@link Double}&gt; lambda
 * (Strategy pattern). The result is only published once <em>both</em> inputs have
 * received at least one numeric message.
 *
 * <p>On construction the agent registers itself as a subscriber of both input topics
 * and as a publisher of the output topic so that {@link Graph#createFromTopics()}
 * can correctly reflect the data flow.
 */
public class BinOpAgent implements Agent {

    private final String name;
    private final String firstTopic;
    private final String secondTopic;
    private final String outputTopic;
    private final BinaryOperator<Double> op;

    /** Latest value received on the first input; {@code null} until first message. */
    private Double x = null;

    /** Latest value received on the second input; {@code null} until first message. */
    private Double y = null;

    /**
     * Creates a BinOpAgent and wires it into the topic graph.
     *
     * @param name         agent identifier (used as graph node label prefix "A")
     * @param firstTopic   name of the first input topic
     * @param secondTopic  name of the second input topic
     * @param outputTopic  name of the topic to publish results to
     * @param op           the binary operation to apply
     */
    public BinOpAgent(String name,
                      String firstTopic,
                      String secondTopic,
                      String outputTopic,
                      BinaryOperator<Double> op) {
        this.name        = name;
        this.firstTopic  = firstTopic;
        this.secondTopic = secondTopic;
        this.outputTopic = outputTopic;
        this.op          = op;

        TopicManager tm = TopicManagerSingleton.get();
        tm.getTopic(firstTopic).subscribe(this);
        tm.getTopic(secondTopic).subscribe(this);
        tm.getTopic(outputTopic).addPublisher(this);
    }

    @Override
    public String getName() { return name; }

    /**
     * Resets both stored inputs to {@code 0.0}.
     */
    @Override
    public void reset() {
        x = 0.0;
        y = 0.0;
    }

    /**
     * Updates the stored value for the incoming topic, then publishes
     * {@code op.apply(x, y)} if both inputs have been received at least once.
     *
     * @param topic name of the publishing topic
     * @param msg   the published message
     */
    @Override
    public void callback(String topic, Message msg) {
        if (topic.equals(firstTopic))       x = msg.asDouble;
        else if (topic.equals(secondTopic)) y = msg.asDouble;

        if (x != null && y != null) {
            double result = op.apply(x, y);
            TopicManagerSingleton.get()
                                 .getTopic(outputTopic)
                                 .publish(new Message(result));
        }
    }

    /**
     * Unsubscribes from both input topics and de-registers from the output topic.
     */
    @Override
    public void close() {
        TopicManager tm = TopicManagerSingleton.get();
        tm.getTopic(firstTopic).unsubscribe(this);
        tm.getTopic(secondTopic).unsubscribe(this);
        tm.getTopic(outputTopic).removePublisher(this);
    }
}

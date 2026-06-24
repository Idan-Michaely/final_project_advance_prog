package test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Decorator that wraps any {@link Agent} and makes its callback non-blocking
 * (Active Object pattern).
 *
 * <p>When a message arrives via {@link #callback}, it is placed on an internal
 * bounded queue and returns immediately. A single background thread drains the
 * queue and forwards each entry to the wrapped agent's callback, guaranteeing
 * that all messages for this agent are processed in the same thread and in
 * arrival order.
 *
 * <p>Call {@link #close()} to gracefully stop the background thread even if it
 * is currently blocked waiting for new messages.
 */
public class ParallelAgent implements Agent {

    /** Pairs a topic name with its message so both can travel through the queue. */
    private static class Entry {
        final String topic;
        final Message msg;

        Entry(String topic, Message msg) {
            this.topic = topic;
            this.msg   = msg;
        }
    }

    private final Agent agent;
    private final BlockingQueue<Entry> queue;
    private final Thread thread;

    /**
     * Creates a ParallelAgent wrapping {@code agent} with an internal queue
     * of the given capacity, and starts the background processing thread.
     *
     * @param agent    the agent whose callback will be executed asynchronously
     * @param capacity maximum number of queued messages before {@link #callback} blocks
     */
    public ParallelAgent(Agent agent, int capacity) {
        this.agent  = agent;
        this.queue  = new ArrayBlockingQueue<>(capacity);
        this.thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Entry e = queue.take();
                    agent.callback(e.topic, e.msg);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        this.thread.start();
    }

    /**
     * Enqueues the (topic, message) pair for asynchronous processing.
     * Returns immediately after placing the entry on the queue.
     *
     * @param topic the name of the publishing topic
     * @param msg   the published message
     */
    @Override
    public void callback(String topic, Message msg) {
        try {
            queue.put(new Entry(topic, msg));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stops the background thread and then closes the wrapped agent.
     * Blocks until the thread has fully terminated.
     */
    @Override
    public void close() {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        agent.close();
    }

    @Override
    public String getName() {
        return agent.getName();
    }

    @Override
    public void reset() {
        agent.reset();
    }
}

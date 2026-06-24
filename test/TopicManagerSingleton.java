package test;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a thread-safe singleton {@link TopicManager} via an eager-initialised
 * inner static class (initialization-on-demand pattern).
 *
 * <p>Usage: {@code TopicManagerSingleton.get().getTopic("myTopic")}
 */
public class TopicManagerSingleton {

    /**
     * Singleton topic registry.
     * Uses a {@link ConcurrentHashMap} so concurrent topic creation is safe.
     */
    public static class TopicManager {

        private static final TopicManager instance = new TopicManager();

        private final ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();

        private TopicManager() {}

        /**
         * Returns the existing topic with the given name, or creates a new one
         * (flyweight pattern).
         *
         * @param name topic name; must be non-null and non-empty
         * @return the corresponding {@link Topic}, or {@code null} for blank names
         */
        public Topic getTopic(String name) {
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            return topics.computeIfAbsent(name, Topic::new);
        }

        /**
         * Returns all currently registered topics.
         *
         * @return live collection of topics
         */
        public Collection<Topic> getTopics() {
            return topics.values();
        }

        /** Removes all topics from the registry. */
        public void clear() {
            topics.clear();
        }
    }

    /**
     * Returns the singleton {@link TopicManager} instance.
     *
     * @return the global topic manager
     */
    public static TopicManager get() {
        return TopicManager.instance;
    }
}

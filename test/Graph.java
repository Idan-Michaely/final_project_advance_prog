package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import test.TopicManagerSingleton.TopicManager;

/**
 * A directed graph representing the computation topology.
 * Extends {@link ArrayList}&lt;{@link Node}&gt; so the graph itself is the node list.
 *
 * <p>Topic nodes are prefixed with {@code T}; agent nodes are prefixed with {@code A}.
 */
public class Graph extends ArrayList<Node> {

    /**
     * Returns {@code true} if any node in the graph can reach a directed cycle.
     */
    public boolean hasCycles() {
        for (Node node : this) {
            if (node.hasCycles()) return true;
        }
        return false;
    }

    /**
     * Rebuilds this graph from the current state of the {@link TopicManager}.
     *
     * <p>For every topic T:
     * <ul>
     *   <li>A topic node "T" + topic.name is created.</li>
     *   <li>For each subscriber of the topic: edge topic-node → agent-node.</li>
     *   <li>For each publisher  of the topic: edge agent-node → topic-node.</li>
     * </ul>
     * Duplicate nodes (same name) are never added twice.
     */
    public void createFromTopics() {
        clear();
        TopicManager tm = TopicManagerSingleton.get();
        Map<String, Node> nodeMap = new HashMap<>();

        for (Topic topic : tm.getTopics()) {
            Node topicNode = nodeMap.computeIfAbsent("T" + topic.name, Node::new);
            if (!contains(topicNode)) add(topicNode);

            for (Agent sub : topic.getSubscribers()) {
                Node agentNode = nodeMap.computeIfAbsent("A" + sub.getName(), Node::new);
                if (!contains(agentNode)) add(agentNode);
                if (!topicNode.getEdges().contains(agentNode))
                    topicNode.addEdge(agentNode);
            }

            for (Agent pub : topic.getPublishers()) {
                Node agentNode = nodeMap.computeIfAbsent("A" + pub.getName(), Node::new);
                if (!contains(agentNode)) add(agentNode);
                if (!agentNode.getEdges().contains(topicNode))
                    agentNode.addEdge(topicNode);
            }
        }
    }
}

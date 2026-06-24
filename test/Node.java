package test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A vertex in the computation graph.
 * Each node has a name, an optional message payload, and a list of outgoing edges.
 */
public class Node {

    private String name;
    private List<Node> edges;
    private Message msg;

    /**
     * Creates a node with the given name and an empty edge list.
     *
     * @param name the node's label (e.g. "TA", "Aplus")
     */
    public Node(String name) {
        this.name  = name;
        this.edges = new ArrayList<>();
    }

    /** @return the node's name */
    public String getName() { return name; }

    /** @param name new node name */
    public void setName(String name) { this.name = name; }

    /** @return list of nodes this node has directed edges to */
    public List<Node> getEdges() { return edges; }

    /** @param edges replacement edge list */
    public void setEdges(List<Node> edges) { this.edges = edges; }

    /** @return the message stored at this node, or {@code null} */
    public Message getMessage() { return msg; }

    /** @param msg message to store at this node */
    public void setMessage(Message msg) { this.msg = msg; }

    /**
     * Adds a directed edge from this node to {@code node}.
     *
     * @param node the destination node
     */
    public void addEdge(Node node) {
        edges.add(node);
    }

    /**
     * Returns {@code true} if a directed cycle is reachable from this node.
     * Uses a depth-first search with a recursion stack to detect back edges.
     *
     * @return {@code true} if this node is part of or can reach a cycle
     */
    public boolean hasCycles() {
        return dfsCycle(this, new HashSet<>(), new HashSet<>());
    }

    private boolean dfsCycle(Node node, Set<Node> visited, Set<Node> stack) {
        visited.add(node);
        stack.add(node);
        for (Node neighbor : node.edges) {
            if (!visited.contains(neighbor)) {
                if (dfsCycle(neighbor, visited, stack)) return true;
            } else if (stack.contains(neighbor)) {
                return true;
            }
        }
        stack.remove(node);
        return false;
    }
}

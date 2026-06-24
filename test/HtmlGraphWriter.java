package test;

import java.util.ArrayList;
import java.util.List;
import test.TopicManagerSingleton.TopicManager;

/**
 * Generates an SVG visualization of the current computation graph.
 *
 * <p>Topic nodes are drawn as blue rectangles; agent nodes as red circles.
 * Directed edges are drawn as arrows from publisher-agent to topic and
 * from topic to subscriber-agent.
 */
public class HtmlGraphWriter {

    // Layout constants
    private static final int NODE_W    = 120;
    private static final int NODE_H    = 40;
    private static final int RADIUS    = 30;
    private static final int H_GAP     = 60;
    private static final int V_GAP     = 80;
    private static final int MARGIN    = 60;

    // Colours
    private static final String TOPIC_FILL  = "#4ECDC4";
    private static final String AGENT_FILL  = "#FF6B6B";
    private static final String STROKE      = "#333333";
    private static final String TEXT_COLOR  = "#FFFFFF";
    private static final String EDGE_COLOR  = "#555555";

    /**
     * Builds the SVG markup lines for a graph and returns them as a list.
     * The caller joins the list with newlines and embeds it in an HTML page.
     *
     * @param graph the graph to render
     * @return list of SVG lines (no surrounding &lt;svg&gt; tag)
     */
    public static List<String> getGraphSVG(Graph graph) {
        List<String> lines = new ArrayList<>();

        if (graph == null || graph.isEmpty()) {
            lines.add("<text x='50%' y='50%' text-anchor='middle' fill='#888' font-size='16'>No graph loaded</text>");
            return lines;
        }

        // Separate topic and agent nodes
        List<Node> topicNodes = new ArrayList<>();
        List<Node> agentNodes = new ArrayList<>();
        for (Node n : graph) {
            if (n.getName().startsWith("T")) topicNodes.add(n);
            else                              agentNodes.add(n);
        }

        // Assign grid positions: agents on left column, topics on right column
        int maxRows = Math.max(topicNodes.size(), agentNodes.size());
        int svgH = MARGIN * 2 + maxRows * (NODE_H + V_GAP);
        int svgW = MARGIN * 2 + 2 * (NODE_W + H_GAP);

        // Build position maps (node name → cx, cy)
        int[][] agentPos = new int[agentNodes.size()][2];
        int[][] topicPos = new int[topicNodes.size()][2];

        int agentX = MARGIN + NODE_W / 2;
        int topicX = MARGIN + NODE_W + H_GAP + NODE_W / 2;

        for (int i = 0; i < agentNodes.size(); i++) {
            agentPos[i][0] = agentX;
            agentPos[i][1] = MARGIN + NODE_H / 2 + i * (NODE_H + V_GAP);
        }
        for (int i = 0; i < topicNodes.size(); i++) {
            topicPos[i][0] = topicX;
            topicPos[i][1] = MARGIN + NODE_H / 2 + i * (NODE_H + V_GAP);
        }

        // SVG header with arrowhead marker
        lines.add("<defs>");
        lines.add("  <marker id='arrow' markerWidth='8' markerHeight='8' refX='6' refY='3' orient='auto'>");
        lines.add("    <path d='M0,0 L0,6 L8,3 z' fill='" + EDGE_COLOR + "'/>");
        lines.add("  </marker>");
        lines.add("</defs>");

        // Draw edges
        for (int ai = 0; ai < agentNodes.size(); ai++) {
            Node agentNode = agentNodes.get(ai);
            int ax = agentPos[ai][0];
            int ay = agentPos[ai][1];

            for (Node edge : agentNode.getEdges()) {
                int ti = indexOfByName(topicNodes, edge.getName());
                if (ti < 0) continue;
                int tx = topicPos[ti][0] - NODE_W / 2;
                int ty = topicPos[ti][1];
                lines.add(edgeLine(ax + RADIUS, ay, tx, ty));
            }
        }
        for (int ti = 0; ti < topicNodes.size(); ti++) {
            Node topicNode = topicNodes.get(ti);
            int tx = topicPos[ti][0];
            int ty = topicPos[ti][1];

            for (Node edge : topicNode.getEdges()) {
                int ai = indexOfByName(agentNodes, edge.getName());
                if (ai < 0) continue;
                int ax = agentPos[ai][0] - RADIUS;
                int ay = agentPos[ai][1];
                lines.add(edgeLine(tx + NODE_W / 2, ty, ax, ay));
            }
        }

        // Draw topic nodes (rectangles)
        TopicManager tm = TopicManagerSingleton.get();
        for (int i = 0; i < topicNodes.size(); i++) {
            Node n = topicNodes.get(i);
            int cx = topicPos[i][0];
            int cy = topicPos[i][1];
            int rx = cx - NODE_W / 2;
            int ry = cy - NODE_H / 2;
            String label = n.getName().substring(1); // strip "T" prefix

            // current value from topic
            Topic t = tm.getTopic(label);
            String val = (t != null && !t.getResult().isEmpty()) ? t.getResult() : "";

            lines.add("<rect x='" + rx + "' y='" + ry + "' width='" + NODE_W + "' height='" + NODE_H + "'"
                    + " rx='6' fill='" + TOPIC_FILL + "' stroke='" + STROKE + "' stroke-width='1.5'/>");
            lines.add("<text x='" + cx + "' y='" + (cy - 4) + "' text-anchor='middle'"
                    + " font-size='12' font-weight='bold' fill='" + TEXT_COLOR + "'>" + escXml(label) + "</text>");
            if (!val.isEmpty()) {
                lines.add("<text x='" + cx + "' y='" + (cy + 12) + "' text-anchor='middle'"
                        + " font-size='10' fill='" + TEXT_COLOR + "'>" + escXml(val) + "</text>");
            }
        }

        // Draw agent nodes (circles)
        for (int i = 0; i < agentNodes.size(); i++) {
            Node n = agentNodes.get(i);
            int cx = agentPos[i][0];
            int cy = agentPos[i][1];
            String label = n.getName().substring(1); // strip "A" prefix

            lines.add("<circle cx='" + cx + "' cy='" + cy + "' r='" + RADIUS + "'"
                    + " fill='" + AGENT_FILL + "' stroke='" + STROKE + "' stroke-width='1.5'/>");
            lines.add("<text x='" + cx + "' y='" + (cy + 4) + "' text-anchor='middle'"
                    + " font-size='10' fill='" + TEXT_COLOR + "'>" + escXml(shortLabel(label)) + "</text>");
        }

        return lines;
    }

    /**
     * Returns an HTML snippet listing all topics and their current values,
     * plus cycle-detection status, suitable for the info panel.
     *
     * @param graph the current graph
     * @return HTML string
     */
    public static String getTopicInfo(Graph graph) {
        TopicManager tm = TopicManagerSingleton.get();
        StringBuilder sb = new StringBuilder();
        sb.append("<table border='1' cellpadding='4' cellspacing='0' style='border-collapse:collapse;width:100%'>");
        sb.append("<tr style='background:#4ECDC4;color:white'><th>Topic</th><th>Current Value</th></tr>");

        for (Topic t : tm.getTopics()) {
            String val = t.getResult().isEmpty() ? "<em>—</em>" : escXml(t.getResult());
            sb.append("<tr><td>").append(escXml(t.name)).append("</td><td>").append(val).append("</td></tr>");
        }
        sb.append("</table>");

        if (graph != null && !graph.isEmpty()) {
            sb.append("<p style='margin-top:8px'>Nodes: ").append(graph.size())
              .append(" &nbsp;|&nbsp; Cycles: ").append(graph.hasCycles() ? "<b style='color:red'>YES</b>" : "none")
              .append("</p>");
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------

    private static String edgeLine(int x1, int y1, int x2, int y2) {
        return "<line x1='" + x1 + "' y1='" + y1 + "' x2='" + x2 + "' y2='" + y2 + "'"
             + " stroke='" + EDGE_COLOR + "' stroke-width='1.5' marker-end='url(#arrow)'/>";
    }

    private static int indexOfByName(List<Node> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /** Trims long agent names for display inside a small circle. */
    private static String shortLabel(String name) {
        return name.length() > 10 ? name.substring(0, 9) + "…" : name;
    }

    private static String escXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}

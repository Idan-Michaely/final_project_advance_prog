package test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import test.RequestParser.RequestInfo;
import test.TopicManagerSingleton.TopicManager;

/**
 * Handles GET /api/update requests.
 *
 * <p>Returns a JSON object with three fields:
 * <ul>
 *   <li>{@code svg}        — full {@code <svg>} element for the current graph</li>
 *   <li>{@code topicsHtml} — HTML table of all topics and their values</li>
 *   <li>{@code log}        — JSON array of recent event-log entries (newest first)</li>
 * </ul>
 *
 * <p>Clients poll this endpoint every 2 seconds and update the DOM in-place,
 * avoiding the page-flicker caused by full iframe reloads.
 */
public class ApiServlet implements Servlet {

    private final Graph graph;

    public ApiServlet(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(RequestInfo ri, OutputStream out) throws IOException {
        graph.createFromTopics();

        // --- SVG ---
        List<String> svgLines = HtmlGraphWriter.getGraphSVG(graph);
        int svgW = 500;
        int svgH = Math.max(300, graph.size() * 80 + 100);
        String svgTag = "<svg xmlns='http://www.w3.org/2000/svg' width='" + svgW
                + "' height='" + svgH + "' style='background:#f9f9f9;border:1px solid #ddd'>"
                + String.join("", svgLines)
                + "</svg>";

        // --- Topics table HTML ---
        TopicManager tm = TopicManagerSingleton.get();
        Collection<Topic> topics = tm.getTopics();
        StringBuilder table = new StringBuilder();
        table.append("<table border='1' cellpadding='6' cellspacing='0'"
                + " style='border-collapse:collapse;width:100%;font-size:13px'>");
        table.append("<tr style='background:#4ECDC4;color:white'>"
                + "<th>Topic</th><th>Last Value</th><th>Status</th></tr>");
        if (topics.isEmpty()) {
            table.append("<tr><td colspan='3' style='text-align:center;color:#888'>"
                    + "No topics — upload a config first.</td></tr>");
        } else {
            for (Topic t : topics) {
                boolean hasValue = !t.getResult().isEmpty();
                String val    = hasValue ? escXml(t.getResult()) : "<em style='color:#aaa'>—</em>";
                String status = hasValue
                        ? "<b style='color:green'>&#10003; Valid</b>"
                        : "<b style='color:red'>&#10007; Invalid</b>";
                table.append("<tr><td><b>").append(escXml(t.name)).append("</b></td>")
                     .append("<td>").append(val).append("</td>")
                     .append("<td>").append(status).append("</td></tr>");
            }
        }
        table.append("</table>");
        if (graph != null && !graph.isEmpty()) {
            table.append("<p style='margin-top:6px;font-size:12px;color:#555'>Nodes: ").append(graph.size())
                 .append(" &nbsp;|&nbsp; Cycles: ")
                 .append(graph.hasCycles() ? "<b style='color:red'>YES</b>" : "none")
                 .append("</p>");
        }

        // --- Log ---
        List<String> logEntries = EventLog.get().getAll();
        StringBuilder logJson = new StringBuilder("[");
        for (int i = 0; i < logEntries.size(); i++) {
            if (i > 0) logJson.append(",");
            logJson.append(jsonStr(logEntries.get(i)));
        }
        logJson.append("]");

        // --- Assemble JSON ---
        String json = "{\"svg\":" + jsonStr(svgTag)
                    + ",\"topicsHtml\":" + jsonStr(table.toString())
                    + ",\"log\":" + logJson + "}";

        byte[] bytes = json.getBytes("UTF-8");
        out.write(("HTTP/1.1 200 OK\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n"
                + "Cache-Control: no-cache\r\n"
                + "Content-Length: " + bytes.length + "\r\n\r\n").getBytes());
        out.write(bytes);
    }

    @Override public void close() throws IOException {}

    /** JSON-escapes a string and wraps it in double quotes. */
    static String jsonStr(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else          sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String escXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

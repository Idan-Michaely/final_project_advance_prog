package test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import test.RequestParser.RequestInfo;
import test.TopicManagerSingleton.TopicManager;

/**
 * Handles GET /publish requests.
 *
 * <p>Query parameters:
 * <ul>
 *   <li>{@code topic}   — topic name(s), comma-separated (e.g. {@code A} or {@code X,Y,Z})</li>
 *   <li>{@code message} — value(s), comma-separated in matching order</li>
 * </ul>
 *
 * <p>Rules:
 * <ul>
 *   <li>Topic names must only contain letters, digits, or underscores.</li>
 *   <li>The topic must already exist in the current configuration — publishing to an
 *       unknown topic returns an error and is blocked.</li>
 *   <li>When multiple topics are given, the number of values must match.</li>
 * </ul>
 *
 * <p>Returns an HTML fragment (not a full display page — display is handled
 * by the static {@code topics.html} file via {@link ApiServlet}).
 */
public class TopicDisplayer implements Servlet {

    private final Graph graph;

    public TopicDisplayer(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(RequestInfo ri, OutputStream out) throws IOException {
        TopicManager tm = TopicManagerSingleton.get();

        String topicParam   = ri.getParameters().get("topic");
        String messageParam = ri.getParameters().get("message");

        String responseBody;

        if (topicParam != null && !topicParam.isEmpty() && messageParam != null) {
            responseBody = publishAndRespond(tm, topicParam, messageParam);
        } else {
            responseBody = "OK";
        }

        byte[] bytes = responseBody.getBytes("UTF-8");
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Length: "
                + bytes.length + "\r\n\r\n").getBytes());
        out.write(bytes);
    }

    private String publishAndRespond(TopicManager tm, String topicParam, String messageParam) {
        // Split on comma to support multi-value publish: topic=X,Y,Z & message=1,2,3
        String[] topicNames = topicParam.split(",");
        String[] values     = messageParam.split(",");

        if (topicNames.length != values.length) {
            return "ERROR: Number of topics (" + topicNames.length
                    + ") does not match number of values (" + values.length + ").";
        }

        Collection<Topic> existing = tm.getTopics();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < topicNames.length; i++) {
            String name = topicNames[i].trim();
            String val  = values[i].trim();

            // Validate name format
            if (!name.matches("[A-Za-z0-9_]+")) {
                result.append("ERROR: Invalid topic name '").append(name)
                      .append("'. Use letters, digits, and underscores only.\n");
                continue;
            }

            // Block publishing to topics not in the current config
            boolean known = false;
            for (Topic t : existing) {
                if (t.name.equals(name)) { known = true; break; }
            }
            if (!known) {
                result.append("ERROR: Topic '").append(name)
                      .append("' does not exist in the current configuration. ")
                      .append("Load a config first, or use only topics defined in it.\n");
                continue;
            }

            // Publish
            tm.getTopic(name).publish(new Message(val));
            EventLog.get().add(name, val);
            result.append("OK: ").append(name).append(" = ").append(val).append("\n");
        }

        graph.createFromTopics();
        return result.toString().trim();
    }

    @Override public void close() throws IOException {}
}

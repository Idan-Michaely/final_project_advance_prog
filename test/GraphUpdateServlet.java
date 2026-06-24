package test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import test.RequestParser.RequestInfo;

/**
 * Handles GET /graph requests.
 *
 * <p>Loads {@code html_files/graph.html}, replaces the placeholders
 * {@code {{SVG_CONTENT}}} and {@code {{GRAPH_INFO}}} with a live SVG
 * rendering and an info table, then returns the result as HTML.
 * Falls back to an inline template if the file is not found.
 */
public class GraphUpdateServlet implements Servlet {

    private final Graph  graph;
    private final String htmlDir;

    public GraphUpdateServlet(Graph graph, String htmlDir) {
        this.graph   = graph;
        this.htmlDir = htmlDir;
    }

    @Override
    public void handle(RequestInfo ri, OutputStream out) throws IOException {
        graph.createFromTopics();

        List<String> svgLines = HtmlGraphWriter.getGraphSVG(graph);
        String svgContent = String.join("\n", svgLines);
        String topicInfo  = HtmlGraphWriter.getTopicInfo(graph);

        int svgW = 500;
        int svgH = Math.max(300, graph.size() * 80 + 100);

        String svgTag = "<svg xmlns='http://www.w3.org/2000/svg' width='" + svgW
                      + "' height='" + svgH + "' style='background:#f9f9f9;border:1px solid #ddd'>\n"
                      + svgContent + "\n</svg>";

        String template = loadTemplate();
        String html = template
                .replace("{{SVG_CONTENT}}", svgTag)
                .replace("{{GRAPH_INFO}}", topicInfo);

        byte[] bytes = html.getBytes("UTF-8");
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Length: "
                + bytes.length + "\r\n\r\n").getBytes());
        out.write(bytes);
    }

    @Override public void close() throws IOException {}

    // -------------------------------------------------------------------------

    private String loadTemplate() {
        File f = new File(htmlDir, "graph.html");
        if (f.exists()) {
            try {
                return new String(Files.readAllBytes(f.toPath()), "UTF-8");
            } catch (IOException ignored) {}
        }
        // Inline fallback
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
             + "<meta http-equiv='refresh' content='2'>"
             + "<title>Graph</title>"
             + "<style>body{font-family:sans-serif;padding:8px;margin:0}"
             + "h3{margin:4px 0}</style></head><body>"
             + "<h3>Computation Graph</h3>"
             + "{{SVG_CONTENT}}"
             + "<hr/>{{GRAPH_INFO}}"
             + "</body></html>";
    }
}

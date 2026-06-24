package test;

/**
 * Entry point for the WebGraph application.
 *
 * <p>Starts an HTTP server on port 8080 and registers four servlets:
 * <ul>
 *   <li>GET  /app/   — {@link HtmlLoader}     — serves static HTML files</li>
 *   <li>GET  /graph  — {@link GraphUpdateServlet} — live SVG graph view</li>
 *   <li>GET  /publish — {@link TopicDisplayer}  — publish a value &amp; view topics</li>
 *   <li>POST /upload — {@link ConfLoader}       — upload a .conf file</li>
 * </ul>
 *
 * <p>Run from the {@code finalproject/} directory so that relative paths
 * ({@code html_files/} and {@code config_files/}) resolve correctly.
 *
 * <p>Open <a href="http://localhost:8080/app/index.html">http://localhost:8080/app/index.html</a>
 * in a browser after starting.
 */
public class Main {

    private static final int    PORT     = 8080;
    private static final int    THREADS  = 5;
    private static final String HTML_DIR = "html_files";

    public static void main(String[] args) throws Exception {
        Graph graph = new Graph();

        ConfLoader confLoader = new ConfLoader(graph);

        MyHTTPServer server = new MyHTTPServer(PORT, THREADS);
        server.addServlet("GET",  "/app/",        new HtmlLoader(HTML_DIR));
        server.addServlet("GET",  "/graph",        new GraphUpdateServlet(graph, HTML_DIR));
        server.addServlet("GET",  "/publish",      new TopicDisplayer(graph));
        server.addServlet("POST", "/upload",       confLoader);
        server.addServlet("GET",  "/api/update",   new ApiServlet(graph));
        server.addServlet("POST", "/expression",   new ExpressionServlet(confLoader));

        server.start();
        System.out.println("Server started on http://localhost:" + PORT);
        System.out.println("Open: http://localhost:" + PORT + "/app/index.html");
        System.out.println("Press Ctrl+C to stop.");
    }
}

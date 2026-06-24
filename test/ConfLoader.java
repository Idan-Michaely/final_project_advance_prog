package test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import test.RequestParser.RequestInfo;
import test.TopicManagerSingleton.TopicManager;

/**
 * Handles POST /upload requests.
 *
 * <p>The request body must contain the raw text of a {@code .conf} file
 * (one agent block per three lines: class name, subs, pubs).
 * On success the servlet:
 * <ol>
 *   <li>Clears all existing topics and agents.</li>
 *   <li>Writes the body to a temporary file and loads it via {@link GenericConfig}.</li>
 *   <li>Rebuilds the computation {@link Graph}.</li>
 *   <li>Returns an HTML page containing an SVG of the new graph.</li>
 * </ol>
 */
public class ConfLoader implements Servlet {

    /** Shared graph instance updated on every upload. */
    private final Graph graph;

    /** Active GenericConfig — kept so its threads can be shut down on re-upload. */
    private GenericConfig activeConfig = null;

    public ConfLoader(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void handle(RequestInfo ri, OutputStream out) throws IOException {
        byte[] body = ri.getContent();

        if (body == null || body.length == 0) {
            sendJson(out, false, "No configuration data received.");
            return;
        }

        try {
            loadConf(new String(body, "UTF-8"));
        } catch (Exception e) {
            sendJson(out, false, e.getMessage());
            return;
        }

        sendJson(out, true, null);
    }

    /**
     * Clears the current config, loads a new one from the given .conf text,
     * and rebuilds the computation graph.  Shared with {@link ExpressionServlet}.
     *
     * @param confText raw .conf file content (3 lines per agent)
     * @throws Exception if the temp file cannot be written or GenericConfig fails
     */
    public synchronized void loadConf(String confText) throws Exception {
        // Write conf text to a temp file (GenericConfig reads from a file path)
        java.io.File tmpFile = java.io.File.createTempFile("upload_", ".conf");
        tmpFile.deleteOnExit();
        java.nio.file.Files.write(tmpFile.toPath(), confText.getBytes("UTF-8"));

        // Shut down previous config and clear all topics
        TopicManager tm = TopicManagerSingleton.get();
        if (activeConfig != null) {
            activeConfig.close();
            activeConfig = null;
        }
        tm.clear();

        // Load new config
        GenericConfig gc = new GenericConfig();
        gc.setConfFile(tmpFile.getAbsolutePath());
        gc.create();

        // Surface any errors collected by GenericConfig
        if (gc.hasErrors()) {
            gc.close(); // shut down any threads that did start
            throw new Exception(String.join("\n", gc.getErrors()));
        }

        activeConfig = gc;

        // Rebuild graph
        graph.createFromTopics();
    }

    @Override
    public void close() throws IOException {
        if (activeConfig != null) {
            activeConfig.close();
            activeConfig = null;
        }
    }

    // -------------------------------------------------------------------------

    private static void sendJson(OutputStream out, boolean ok, String error) throws IOException {
        String json = ok ? "{\"ok\":true}"
                        : "{\"ok\":false,\"error\":" + ApiServlet.jsonStr(error) + "}";
        byte[] bytes = json.getBytes("UTF-8");
        out.write(("HTTP/1.1 200 OK\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n"
                + "Content-Length: " + bytes.length + "\r\n\r\n").getBytes());
        out.write(bytes);
    }

}

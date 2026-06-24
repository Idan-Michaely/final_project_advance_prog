package test;

import java.io.IOException;
import java.io.OutputStream;
import test.RequestParser.RequestInfo;

/**
 * Handles POST /expression requests.
 *
 * <p>The request body contains math expressions (one per line), e.g.:
 * <pre>
 * C = A + B
 * D = inc(C)
 * E = max(A, C)
 * </pre>
 *
 * <p>The expressions are parsed by {@link ExpressionParser} into a .conf string
 * and then loaded via {@link ConfLoader#loadConf(String)}.
 *
 * <p>Returns JSON: {@code {"ok":true}} on success,
 * or {@code {"ok":false,"error":"..."}} on failure.
 */
public class ExpressionServlet implements Servlet {

    private final ConfLoader loader;

    public ExpressionServlet(ConfLoader loader) {
        this.loader = loader;
    }

    @Override
    public void handle(RequestInfo ri, OutputStream out) throws IOException {
        byte[] body = ri.getContent();
        String text = (body != null && body.length > 0) ? new String(body, "UTF-8").trim() : "";

        if (text.isEmpty()) {
            sendJson(out, false, "No expressions provided.");
            return;
        }

        String confText;
        try {
            confText = ExpressionParser.parse(text);
        } catch (IllegalArgumentException e) {
            sendJson(out, false, e.getMessage());
            return;
        }

        try {
            loader.loadConf(confText);
        } catch (Exception e) {
            sendJson(out, false, "Config load failed: " + e.getMessage());
            return;
        }

        sendJson(out, true, null);
    }

    @Override public void close() throws IOException {}

    // -------------------------------------------------------------------------

    private static void sendJson(OutputStream out, boolean ok, String error) throws IOException {
        String json;
        if (ok) {
            json = "{\"ok\":true}";
        } else {
            json = "{\"ok\":false,\"error\":" + ApiServlet.jsonStr(error) + "}";
        }
        byte[] bytes = json.getBytes("UTF-8");
        out.write(("HTTP/1.1 200 OK\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n"
                + "Content-Length: " + bytes.length + "\r\n\r\n").getBytes());
        out.write(bytes);
    }
}

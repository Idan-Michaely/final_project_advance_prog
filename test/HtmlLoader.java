package test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import test.RequestParser.RequestInfo;

/**
 * Serves static files from the {@code html_files/} directory.
 *
 * <p>Registered on prefix {@code /app/}. A request for {@code /app/form.html}
 * reads {@code html_files/form.html} and returns it with the appropriate
 * Content-Type header.
 */
public class HtmlLoader implements Servlet {

    private final String htmlDir;

    /**
     * @param htmlDir path to the directory that contains the HTML/CSS/JS files
     */
    public HtmlLoader(String htmlDir) {
        this.htmlDir = htmlDir;
    }

    @Override
    public void handle(RequestInfo ri, OutputStream out) throws IOException {
        // Strip leading "/app/" to get the filename
        String uri  = ri.getUri();
        String path = uri.startsWith("/app/") ? uri.substring(5) : uri;
        if (path.isEmpty() || path.equals("/")) path = "index.html";

        File file = new File(htmlDir, path);

        if (!file.exists() || !file.isFile()) {
            String body = "404 Not Found: " + path;
            String response = "HTTP/1.1 404 Not Found\r\n"
                    + "Content-Type: text/plain\r\n"
                    + "Content-Length: " + body.length() + "\r\n\r\n" + body;
            out.write(response.getBytes());
            return;
        }

        byte[] content = Files.readAllBytes(file.toPath());
        String contentType = detectContentType(path);

        out.write(("HTTP/1.1 200 OK\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Content-Length: " + content.length + "\r\n\r\n").getBytes());
        out.write(content);
    }

    @Override
    public void close() throws IOException {}

    private static String detectContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css"))  return "text/css";
        if (filename.endsWith(".js"))   return "application/javascript";
        if (filename.endsWith(".svg"))  return "image/svg+xml";
        if (filename.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }
}

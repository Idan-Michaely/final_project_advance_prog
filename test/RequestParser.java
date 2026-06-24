package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses raw HTTP/1.1 requests from a {@link BufferedReader} into a structured
 * {@link RequestInfo} object.
 *
 * <h3>Expected request format</h3>
 * <pre>
 * METHOD /path?key=value HTTP/1.1\n
 * Header: value\n
 * ...\n
 * \n
 * bodyParam=value\n
 * \n
 * body content line\n
 * \n
 * </pre>
 * The body is split into two sections by an empty line:
 * section one contains {@code key=value} parameter lines that are merged into
 * the parameter map; section two is the raw content (each line re-terminated
 * with {@code \n}).
 */
public class RequestParser {

    /**
     * Parses one HTTP request from the given reader.
     *
     * @param reader the buffered input; must be positioned at the start of the request
     * @return a fully populated {@link RequestInfo}
     * @throws IOException if the reader throws during reading
     */
    public static RequestInfo parseRequest(BufferedReader reader) throws IOException {

        // --- 1. First line: METHOD URI HTTP/version ---
        String firstLine = reader.readLine();
        if (firstLine == null || firstLine.isEmpty())
            throw new IOException("Empty or malformed HTTP request");

        String[] firstParts = firstLine.split(" ", 3);
        String httpCommand = firstParts[0];
        String uri         = firstParts[1];

        // --- 2. URI → segments + query parameters ---
        String[] uriParts = uri.split("\\?", 2);
        String path = uriParts[0];

        List<String> segList = new ArrayList<>();
        for (String seg : path.split("/")) {
            if (!seg.isEmpty()) segList.add(seg);
        }
        String[] uriSegments = segList.toArray(new String[0]);

        Map<String, String> params = new HashMap<>();
        if (uriParts.length > 1) {
            for (String pair : uriParts[1].split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) params.put(kv[0], kv[1]);
            }
        }

        // --- 3. Headers: read until blank line, capture Content-Length ---
        String line;
        int contentLength = -1;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        // --- 4. Body ---
        // Strategy:
        //  • Content-Length: 0  → no body
        //  • Content-Length: N  → read exactly N chars (handles browser POST uploads)
        //  • No Content-Length + body-capable method → old format (shutdownOutput tests)
        //  • GET/HEAD/DELETE with no Content-Length → no body
        byte[] content;
        boolean hasBody = httpCommand.equals("POST") || httpCommand.equals("PUT")
                       || httpCommand.equals("PATCH");

        if (contentLength == 0) {
            content = new byte[0];
        } else if (contentLength > 0) {
            // Standard HTTP body: read exactly contentLength chars
            char[] buf = new char[contentLength];
            int total = 0;
            while (total < contentLength) {
                int n = reader.read(buf, total, contentLength - total);
                if (n < 0) break;
                total += n;
            }
            content = new String(buf, 0, total).getBytes("UTF-8");
        } else if (hasBody) {
            // No Content-Length but body expected (assignment test format):
            // Section 1 (before blank): key=value params; Section 2: raw content.
            // Terminated by second blank line or EOF (caller must shutdownOutput).
            boolean passedSeparator = false;
            StringBuilder contentBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (!passedSeparator) passedSeparator = true;
                    else break;
                } else if (!passedSeparator) {
                    int eq = line.indexOf('=');
                    if (eq >= 0) params.put(line.substring(0, eq), line.substring(eq + 1));
                } else {
                    contentBuilder.append(line).append('\n');
                }
            }
            content = contentBuilder.toString().getBytes("UTF-8");
        } else {
            content = new byte[0];
        }

        return new RequestInfo(httpCommand, uri, uriSegments, params, content);
    }

    /**
     * Immutable holder for a parsed HTTP request.
     */
    public static class RequestInfo {

        private final String httpCommand;
        private final String uri;
        private final String[] uriSegments;
        private final Map<String, String> parameters;
        private final byte[] content;

        /**
         * @param httpCommand HTTP method (GET, POST, …)
         * @param uri         full URI including query string
         * @param uriSegments path segments split by {@code /}, empty segments excluded
         * @param parameters  merged query-string and body parameters
         * @param content     raw body content bytes
         */
        public RequestInfo(String httpCommand,
                           String uri,
                           String[] uriSegments,
                           Map<String, String> parameters,
                           byte[] content) {
            this.httpCommand  = httpCommand;
            this.uri          = uri;
            this.uriSegments  = uriSegments;
            this.parameters   = parameters;
            this.content      = content;
        }

        /** @return the HTTP method, e.g. "GET" */
        public String getHttpCommand() { return httpCommand; }

        /** @return the full URI including any query string */
        public String getUri() { return uri; }

        /** @return path segments (empty segments and query string excluded) */
        public String[] getUriSegments() { return uriSegments; }

        /** @return merged query-string and body parameter map */
        public Map<String, String> getParameters() { return parameters; }

        /** @return raw content bytes from the body */
        public byte[] getContent() { return content; }
    }
}

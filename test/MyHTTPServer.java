package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import test.RequestParser.RequestInfo;

/**
 * A simple multithreaded HTTP/1.1 server.
 *
 * <p>The server runs on a dedicated thread (the object itself extends
 * {@link Thread}).  Incoming connections are dispatched to a fixed-size
 * thread pool.  Servlet routing uses a longest-prefix match on the request
 * URI path.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Register servlets with {@link #addServlet}.</li>
 *   <li>Call {@link #start()} (inherited from {@link Thread}) to open the
 *       port and begin accepting connections.</li>
 *   <li>Call {@link #close()} to stop accepting, shut down the thread pool,
 *       and release the port.</li>
 * </ol>
 */
public class MyHTTPServer extends Thread implements HTTPServer {

    private final int port;
    private final ExecutorService pool;
    private volatile boolean closed = false;
    private volatile ServerSocket serverSocket;

    /** Registered servlets keyed by URI prefix, one map per HTTP method. */
    private final Map<String, Servlet> gets    = new ConcurrentHashMap<>();
    private final Map<String, Servlet> posts   = new ConcurrentHashMap<>();
    private final Map<String, Servlet> deletes = new ConcurrentHashMap<>();

    /**
     * Creates the server but does not yet open the port.
     * Call {@link #start()} to begin accepting connections.
     *
     * @param port     TCP port to listen on
     * @param nThreads maximum number of concurrent client-handler threads
     */
    public MyHTTPServer(int port, int nThreads) {
        this.port = port;
        this.pool = Executors.newFixedThreadPool(nThreads);
    }

    /**
     * Registers a servlet for the given HTTP method and URI prefix.
     *
     * @param httpCommand HTTP method ("GET", "POST", "DELETE")
     * @param uri         URI prefix to match (longest prefix wins at dispatch)
     * @param s           the servlet to invoke
     */
    @Override
    public void addServlet(String httpCommand, String uri, Servlet s) {
        Map<String, Servlet> map = getMap(httpCommand);
        if (map != null) map.put(uri, s);
    }

    /**
     * Removes a previously registered servlet.
     *
     * @param httpCommand HTTP method
     * @param uri         URI prefix used when registering
     */
    @Override
    public void removeServlet(String httpCommand, String uri) {
        Map<String, Servlet> map = getMap(httpCommand);
        if (map != null) map.remove(uri);
    }

    /**
     * Server main loop: accepts connections and dispatches each to the thread pool.
     * A 1-second socket timeout lets the loop check the {@code closed} flag regularly.
     */
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new java.net.InetSocketAddress(port));
            serverSocket.setSoTimeout(1000);

            while (!closed) {
                try {
                    Socket client = serverSocket.accept();
                    pool.submit(() -> handleClient(client));
                } catch (SocketTimeoutException e) {
                    // Normal: wake up to check closed flag
                } catch (SocketException e) {
                    if (closed) break; // ServerSocket was deliberately closed
                }
            }
        } catch (IOException e) {
            if (!closed) System.err.println("MyHTTPServer error: " + e.getMessage());
        } finally {
            pool.shutdown();
            try {
                pool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Signals the server to stop accepting new connections and shuts down
     * the thread pool.  Returns immediately; use {@link #join()} if you need
     * to wait for full termination.
     */
    @Override
    public void close() {
        closed = true;
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException e) { /* ignore */ }
        }
        pool.shutdown();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void handleClient(Socket client) {
        try {
            BufferedReader in  = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream   out = client.getOutputStream();

            RequestInfo ri = RequestParser.parseRequest(in);
            if (ri == null) return;

            Servlet servlet = findServlet(ri.getHttpCommand(), ri.getUri());
            if (servlet != null) {
                servlet.handle(ri, out);
            } else {
                out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            }
            out.flush();
        } catch (IOException e) {
            // Client disconnected or parse error — silently skip
        } finally {
            try { client.close(); } catch (IOException e) { /* ignore */ }
        }
    }

    /**
     * Finds the best-matching servlet using longest-prefix matching on the
     * request path (the part of the URI before any {@code ?}).
     *
     * @param command HTTP method string
     * @param uri     full URI from the request line
     * @return matching servlet, or {@code null} if none registered
     */
    private Servlet findServlet(String command, String uri) {
        Map<String, Servlet> map = getMap(command);
        if (map == null) return null;

        String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;

        String bestKey = null;
        for (String key : map.keySet()) {
            if (path.startsWith(key)) {
                if (bestKey == null || key.length() > bestKey.length()) {
                    bestKey = key;
                }
            }
        }
        return bestKey != null ? map.get(bestKey) : null;
    }

    private Map<String, Servlet> getMap(String command) {
        if (command == null) return null;
        switch (command.toUpperCase()) {
            case "GET":    return gets;
            case "POST":   return posts;
            case "DELETE": return deletes;
            default:       return null;
        }
    }
}

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Http implements HttpHandler {
    private Path httpDocs;
    private String defaultExpand;

    public Http(String httpDocs, String defaultRootFolder, String defaultFirstPage) {
        this.httpDocs = Paths.get(httpDocs).normalize();
        Path test = this.httpDocs.resolve(defaultRootFolder).resolve(defaultFirstPage).toAbsolutePath();
        if (!test.toFile().exists()) {
            System.err.println("Error: could not find " + test);
            System.exit(1);
        }

        defaultExpand = Paths.get(defaultRootFolder, defaultFirstPage).toString();
    }

    public Http(String httpDocs) {
        this(httpDocs, ".");
    }
    public Http(String httpDocs, String defaultRootFolder) { this(httpDocs, defaultRootFolder, "index.html"); }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        // System.out.println("HTTP: " + httpExchange.getRemoteAddress());
        URI requestURI = httpExchange.getRequestURI().normalize();
        // System.out.println(requestURI);

        String method = httpExchange.getRequestMethod();
        // Check if method supported
        switch (method) {
            case "GET":
            case "HEAD":
                break;

            default:
                send(httpExchange, 501, "Not implemented");
        }

        Headers headers =  httpExchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");

        Path resourcePath = getResourcePath(requestURI);
        // System.out.println(resourcePath);
        try {
            String type = Files.probeContentType(resourcePath);

            headers.add("Content-Type", type);

            // Reply to the method's request
            assert (method.equals("GET") || method.equals("HEAD"));
            switch (method) {
                case "GET":
                    byte[] data = Files.readAllBytes(resourcePath);
                    send(httpExchange, 200, data);
                    break;

                case "HEAD":
                    send(httpExchange, 200);
                    break;

                default:
                    assert false : "This shouldn't happen, since unsupported methods are screened out in the previous switch.";
            }


        } catch (Exception e) {
            // URI not found
            System.out.println("Not found URI: " + requestURI);
            System.out.println(" Error msg: " + e.getMessage());
            byte[] response = ("\"" + requestURI + "\" not found").getBytes();
            send(httpExchange, 404, "\"" + requestURI + "\" not found");
            httpExchange.sendResponseHeaders(404, response.length);

            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response);
            }
        }

    }

    static void send(HttpExchange t, int status, String data) throws IOException {
        send(t, status, data.getBytes());
    }

    static void send(HttpExchange t, int status, byte[] data) throws IOException {
        t.sendResponseHeaders(status, data.length);

        try (OutputStream os = t.getResponseBody()) {
            os.write(data);
        }

        t.close();
    }

    static void send(HttpExchange t, int status) throws IOException {
        t.sendResponseHeaders(status, -1);
        t.close();
    }

    public Path getResourcePath(URI uri) {
        String path = uri.getPath();
        if (path.equals("/"))
            path = defaultExpand;

        if (path.charAt(0) == '/')
            path = path.substring(1);

        return httpDocs.resolve(path);
    }
}

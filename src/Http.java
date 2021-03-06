import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Http {
    private Path httpDocs;
    private String defaultExpand;
    private UserSessions authTokens;

    public Http(String httpDocs, String defaultRootFolder, String defaultFirstPage, UserSessions authTokens) {
        this.authTokens = authTokens;
        this.httpDocs = Paths.get(httpDocs).normalize();
        Path test = this.httpDocs.resolve(defaultRootFolder).resolve(defaultFirstPage).toAbsolutePath();
        if (!test.toFile().exists()) {
            System.err.println("Error: could not find " + test);
            System.exit(1);
        }

        defaultExpand = Paths.get(defaultRootFolder, defaultFirstPage).toString();
    }

    public Http(String httpDocs, UserSessions authTokens) {
        this(httpDocs, ".", authTokens);
    }
    public Http(String httpDocs, String defaultRootFolder, UserSessions authTokens) { this(httpDocs, defaultRootFolder, "index.html", authTokens); }

    public void handle(HttpExchange httpExchange) throws IOException {
        URI requestURI = httpExchange.getRequestURI().normalize();
        Headers h = httpExchange.getRequestHeaders();

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

        try {
            String type = Files.probeContentType(resourcePath);
            if (type == null){
                headers.add("Content-Type", getMIME(resourcePath.toString()));
            }else {
                headers.add("Content-Type", type);
            }
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

    static String getMIME(String filePath) {
        String extension;
        {
            int dotIndex = filePath.lastIndexOf('.');
            if (dotIndex < 0) return "application/octet-stream";
            extension = filePath.substring(dotIndex + 1);
        }

        switch (extension.toLowerCase()) {
            case "html":
                return "text/plain";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";

            case "png":
                return "image/png";
            case "jpeg":
            case "jpg":
                return "image/jpeg";
            case "bmp":
                return "image/bmp";

            default:
                return "application/octet-stream";
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
        if (path.startsWith("/login")) {
            path = path.substring("/login".length());
        }

        if (path.equals("") || path.equals("/"))
            path = defaultExpand;

        if (path.charAt(0) == '/')
            path = path.substring(1);

        return httpDocs.resolve(path);
    }
}

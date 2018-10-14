import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Http implements HttpHandler {
    Path httpDocs;

    public Http(String httpDocs) {
        this.httpDocs = Paths.get(httpDocs).normalize();
        // System.out.println("httpDocs\\ = " + httpDocs);
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        // System.out.println("HTTP: " + httpExchange.getRemoteAddress());
        URI requestURI = httpExchange.getRequestURI().normalize();
        // System.out.println(requestURI);

        String path = requestURI.getPath();
        try {
            if (path.equals("/"))
                path = "/Login Page/login.html";

            if (path.charAt(0) == '/')
                path = path.substring(1);

            Path resourcePath = httpDocs.resolve(path);
            // System.out.println(resourcePath);

            byte[] data = Files.readAllBytes(resourcePath);
            String type = Files.probeContentType(resourcePath);

            Headers headers =  httpExchange.getResponseHeaders();
            headers.add("Content-Type", type);

            httpExchange.sendResponseHeaders(200, data.length);
            try(
                    OutputStream os = httpExchange.getResponseBody()
            ) {
                os.write(data);
            }
        } catch (Exception e) {
            // URI not found
            System.out.println("Not found: " + path);
            System.out.println(" from URI: " + requestURI);
            System.out.println(" Error msg: " + e.getMessage());
            byte[] response = ("\"" + requestURI + "\" not found").getBytes();
            httpExchange.sendResponseHeaders(404, response.length);

            try (OutputStream os = httpExchange.getResponseBody()) {
                os.write(response);
            }
        }

        httpExchange.close();
    }
}

import com.sun.net.httpserver.*;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Executor;

public class Server {
    public final static int defaultHttpsPort = 443;
    public final static int defaultHttpPort = 80;
    public final static String defaultHttpDocs = "CS307 Front-end";

    private static void usage(int exitStatus) {
        /*
            You'll need to set up in Intellij's cmd args where "InterLinked/CS307 Front-end" is
            Run -> Edit Configurations
            Program arguments: <folder path>
         */
        System.out.printf(
                "usage: Server [<httpDocsFolder>] [<port>]\n" +
                        "default values:\n" +
                        "   port = %d\n" +
                        "   httpDocsFolder = %s\n",
                defaultHttpPort,
                defaultHttpDocs
        );

        System.exit(exitStatus);
    }

    public static void main(String[] args) {
        int port = defaultHttpsPort;
        String httpDocs = defaultHttpDocs;

        if (args.length > 2)
            usage(1);

        for (String arg : args) {
            switch (arg) {
                case "/?":
                case "-?":
                case "\\?":
                    usage(0);
                    break;

                default:
                    try {
                        port = Integer.decode(arg);
                    } catch (Exception e) {
                        httpDocs = arg;
                    }
            }
        }

        try {
            initHttpsServer(port, httpDocs);

            initHttpRedirect();

            System.out.println("Server started on port " + port);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initHttpRedirect() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/", t -> {
            Headers headers = t.getResponseHeaders();
            String requestURI = t.getRequestURI().toString();
            String response = "https://localhost" + requestURI;
            headers.set("Location", response);
            t.sendResponseHeaders(302, 0);

            System.out.println("Http request redirected to https...");
        });
        server.setExecutor(new HttpThreadCreator());
        server.start();
    }

    private static void initHttpsServer(int port, String httpDocs) throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 0);

        SSLContext sslContext = SSLContext.getInstance("TLS");

        // initialise the keystore
        char[] password = "interlinked".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream("keystore");
        ks.load(fis, password);

        // setup the key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        // setup the trust manager factory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                try {
                    // initialise the SSL context
                    SSLContext c = SSLContext.getDefault();
                    SSLEngine engine = c.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    // get the default parameters
                    SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                    params.setSSLParameters(defaultSSLParameters);
                } catch (Exception e) {
                    System.out.println("Failed to create https server");
                }
            }
        });

        server.createContext("/", new EndpointHandler(httpDocs));
        server.setExecutor(new HttpThreadCreator());

        server.start();
    }
}

// Process the HTTP requests on a new thread
class HttpThreadCreator implements Executor {
    @Override
    public void execute(@NotNull Runnable command) {
        new Thread(command).start();
    }
}
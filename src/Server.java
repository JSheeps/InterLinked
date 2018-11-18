import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

public class Server {
    public final static int defaultPort = 80;
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
                defaultPort,
                defaultHttpDocs
        );

        System.exit(exitStatus);
    }

    public static void main(String[] args) {
        int port = defaultPort;
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
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new EndpointHandler(httpDocs));
            server.setExecutor(new HttpThreadCreator());

            server.start();
            System.out.println("Server started on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// Process the HTTP requests on a new thread
class HttpThreadCreator implements Executor {
    @Override
    public void execute(@NotNull Runnable command) {
        new Thread(command).start();
    }
}
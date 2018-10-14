import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

public class Server {
    private static void usage() {
        /*
            You'll need to set up in Intellij's cmd args where "InterLinked/CS307 Front-end" is
            Run -> Edit Configurations
            Program arguments: <folder path>
         */
        System.out.println(
                "usage: Server <httpDocsFolder> [<port>]"
        );

        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length == 0)
            usage();

        int port = 80;
        if (args.length != 1) {
            try { port = Integer.decode(args[1]); }
            catch (Exception e) {
                System.out.println("Error: argument should be port. Defaulting to " + port);
            }
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
            server.createContext("/", new Http(args[0]));
            server.createContext("/data", new WebAPI());
            server.setExecutor(new HttpThreadCreator());

            server.start();

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
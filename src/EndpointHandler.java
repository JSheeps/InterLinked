//import com.sun.net.httpserver.HttpExchange;
//import com.sun.net.httpserver.HttpHandler;
//
//import java.io.IOException;
//import java.util.concurrent.Semaphore;
//
//public class EndpointHandler implements HttpHandler {
//    WebAPI webAPI;
//    Http http;
//    Debug debug;
//    Semaphore semaphore;
//
//    public EndpointHandler(String httpDocs) {
//        debug = new Debug(true, false);
//        UserSessions authTokens = new UserSessions(debug);
//        webAPI = new WebAPI(authTokens);
//        http = new Http(httpDocs, authTokens);
//        semaphore = new Semaphore(1);
//    }
//
//    @Override
//    public void handle(HttpExchange t) throws IOException {
//        String uri = t.getRequestURI().toString();
//        debug.logVerbose("~~~~New Request URI: " + uri);
//
//        if(uri.startsWith("/data"))
//            webAPI.handle(t);
//        else if(uri.startsWith("/login")) {
//            webAPI.serviceLogIn(t);
//            http.handle(t);
//        }
//        else
//            http.handle(t);
//
//    }
//}

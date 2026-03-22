package main.java.com.betting.handler;

import main.java.com.betting.manager.SessionManager;
import main.java.com.betting.manager.StakeManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class RootHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(RootHandler.class.getName());

    private static final String UNSUPPORTED_REQUEST_METHOD = "unsupported request method : ";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            if (path.endsWith("/session")) {
                processSessionRequest(exchange, path);
            } else if (path.endsWith("/stake")) {
                processStakeRequest(exchange, path);
            } else if (path.endsWith("/highstakes")) {
                processHighStakeRequest(exchange, path);
            } else {
                exchange.sendResponseHeaders(404, 0);
            }
        } finally {
            exchange.close();
        }
    }


    /**
     *  GET /<customerid>/session
     * @param exchange request context
     * @param path the path in request url
     */
    private void processSessionRequest(HttpExchange exchange, String path) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            logger.severe(UNSUPPORTED_REQUEST_METHOD + exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String[] arr = path.split("/");
        try {
            int cid = Integer.parseInt(arr[1]);
            logger.info("Start getting session for user " + cid);
            String tk = SessionManager.getUserSession(cid);
            byte[] b = tk.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, b.length);
            exchange.getResponseBody().write(b);
        } catch (Exception e) {
            logger.severe("Failed to get user session due to " + e.getMessage());
            exchange.sendResponseHeaders(400, -1);
        }
    }

    /**
     * POST /<betofferid>/stake?sessionkey=<sessionkey>
     * @param exchange request context
     * @param path the path in request url
     */
    private void processStakeRequest(HttpExchange exchange, String path) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            logger.severe(UNSUPPORTED_REQUEST_METHOD + exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String query = exchange.getRequestURI().getQuery();

        if (query == null) {
            logger.severe("sessionKey is mandatory.");
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        String[] params = query.split("=");
        if (params.length < 2 || params[1].trim().isEmpty()) {
            logger.severe("sessionKey can not be null.");
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        String sessionKey = params[1];

        Integer cid = SessionManager.findCustomerByKey(sessionKey);
        if (cid == null) {
            logger.severe("Invalid / expired sessionKey.");
            exchange.sendResponseHeaders(401, -1);
            return;
        }

        try {
            int bid = Integer.parseInt(path.split("/")[1]);
            BufferedReader r = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
            int stake = Integer.parseInt(r.readLine().trim());
            logger.info(String.format("Add stake start. customerId = %s, betOfferId = %s, stake = %s", cid, bid, stake));
            StakeManager.addStake(bid, cid, stake);
            exchange.sendResponseHeaders(204, -1);
        } catch (Exception e) {
            logger.severe("Failed to add stake for customer." + e.getMessage());
            exchange.sendResponseHeaders(400, -1);
        }
    }

    /**
     * GET /<betofferid>/highstakes
     * @param exchange request context
     * @param path the path in request url
     */
    private void processHighStakeRequest(HttpExchange exchange, String path) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            logger.severe(UNSUPPORTED_REQUEST_METHOD + exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            int bid = Integer.parseInt(path.split("/")[1]);
            String response = StakeManager.getHighStakes(bid);
            byte[] b = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, b.length);
            exchange.getResponseBody().write(b);
        } catch (Exception e) {
            logger.severe("Failed to get high stacks list due to" + e.getMessage());
            exchange.sendResponseHeaders(400, -1);
        }
    }
}

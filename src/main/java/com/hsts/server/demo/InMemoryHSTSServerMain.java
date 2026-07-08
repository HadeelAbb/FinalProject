package com.hsts.server.demo;

import com.hsts.client.network.MockServerSimulator;
import com.hsts.server.network.HSTSServer;
import com.hsts.server.network.ServerRequestRouter;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Request;
import com.hsts.shared.net.Response;

/**
 * Demo/test server for Partner 2 + Partner 3 integration.
 *
 * This starts a real OCSF HSTSServer on localhost:3000 and routes requests
 * to the existing in-memory MockServerSimulator. Use it when Partner 1's
 * database/controllers are not available yet.
 *
 * Production integration should replace MockServerSimulator with Partner 1's
 * LoginServerController and QuestionServerController using the same
 * ServerRequestRouter registration pattern shown here.
 */
public class InMemoryHSTSServerMain {

    private static final int PORT = 3000;

    public static void main(String[] args) throws Exception {
        MockServerSimulator simulator = new MockServerSimulator();
        ServerRequestRouter router = new ServerRequestRouter();

        for (Command command : Command.values()) {
            router.registerHandler(command, request -> attachRequestId(
                    simulator.process(request.getCommand(), request.getPayload()),
                    request
            ));
        }

        HSTSServer server = new HSTSServer(PORT);
        server.setRouter(router);
        server.startServer();
    }

    private static Response attachRequestId(Response response, Request request) {
        if (response == null) {
            return Response.failure(request.getCommand(), "No response from in-memory server", request.getRequestId());
        }
        response.setRequestId(request.getRequestId());
        if (response.getCommand() == null) {
            response.setCommand(request.getCommand());
        }
        return response;
    }
}

package com.hsts.server.network;

import com.hsts.shared.net.Command;
import com.hsts.shared.net.Request;
import com.hsts.shared.net.Response;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/** Routes incoming OCSF requests to Partner 1 server controllers. */
public class ServerRequestRouter {

    private final Map<Command, Function<Request, Response>> handlers = new EnumMap<>(Command.class);

    public void registerHandler(Command command, Function<Request, Response> handler) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        handlers.put(command, handler);
    }

    public Response route(Request request) {
        if (request == null) {
            return Response.failure(null, "Request cannot be null", null);
        }
        if (request.getCommand() == null) {
            return Response.failure(null, "Request command cannot be null", request.getRequestId());
        }

        Function<Request, Response> handler = handlers.get(request.getCommand());
        if (handler == null) {
            return Response.failure(request.getCommand(),
                    "No handler registered for command: " + request.getCommand(),
                    request.getRequestId());
        }

        try {
            Response response = handler.apply(request);
            if (response == null) {
                return Response.failure(request.getCommand(), "Handler returned no response", request.getRequestId());
            }
            return response;
        } catch (Exception exception) {
            return Response.failure(request.getCommand(),
                    "Handler error: " + exception.getMessage(),
                    request.getRequestId());
        }
    }
}

package com.hsts.shared.net;

import java.io.Serializable;

public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private Command command;
    private Object payload;
    private String message;
    private String requestId;

    public Response() {
    }

    public Response(boolean success, Command command, Object payload, String message, String requestId) {
        this.success = success;
        this.command = command;
        this.payload = payload;
        this.message = message;
        this.requestId = requestId;
    }

    public static Response success(Command command, Object payload, String message, String requestId) {
        return new Response(true, command, payload, message, requestId);
    }

    public static Response failure(Command command, String message, String requestId) {
        return new Response(false, command, null, message, requestId);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}

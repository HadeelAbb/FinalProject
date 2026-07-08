package com.hsts.shared.net;

import java.io.Serializable;

public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private Command command;
    private Object payload;
    private String requestId;

    public Request() {
    }

    public Request(Command command, Object payload, String requestId) {
        this.command = command;
        this.payload = payload;
        this.requestId = requestId;
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}

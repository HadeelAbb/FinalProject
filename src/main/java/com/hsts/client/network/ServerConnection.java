package com.hsts.client.network;

import com.hsts.shared.net.Command;

/**
 * What LoginClientController / QuestionClientController actually depend on.
 * Two implementations: MockServerConnection (in-process, for testing without
 * a live server) and RealServerConnection (wraps Partner 2's real OCSF
 * HSTSClient). Controllers don't know or care which one they're talking to.
 */
public interface ServerConnection {
    void registerHandler(Command command, ResponseHandler handler);

    void sendToServer(Command command, Object payload);
}

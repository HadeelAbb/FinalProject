package com.hsts.server.network;

import com.hsts.shared.net.Command;
import com.hsts.shared.net.ExamEvent;
import com.hsts.shared.net.Response;

/**
 * Generalizes the QUESTIONS_CHANGED broadcast pattern already used in
 * MainServerApp (server.sendToAllClients(...)) into a single place that
 * exam/grading/approval logic can publish to, instead of every controller
 * reaching into HSTSServer directly.
 *
 * When {@link ExamEvent#getTargetUserId()} is set, the event is delivered
 * only to that user's connection via {@link ConnectionRegistry}. Otherwise
 * it is broadcast to every connected client.
 */
public class EventBus {

    private final HSTSServer server;

    public EventBus(HSTSServer server) {
        this.server = server;
    }

    public void publish(ExamEvent event) {
        if (event == null) {
            return;
        }

        Response response = Response.success(Command.EXAM_EVENT, event, event.getMessage(), null);
        String targetUserId = event.getTargetUserId();

        if (targetUserId != null && !targetUserId.isBlank()) {
            if (server != null) {
                server.sendToUser(targetUserId, response);
            }
        } else if (server != null) {
            server.sendToAllClients(response);
        }
    }
}

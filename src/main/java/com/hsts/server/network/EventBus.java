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
 * PARTNER 2 TODO: right now publish() broadcasts to every connected
 * client (same as QUESTIONS_CHANGED does today) regardless of
 * event.getTargetUserId(). To do targeted delivery (e.g. only notify the
 * one teacher whose exam got approved) you'll need a userId -> connection
 * registry, populated on LOGIN and cleared on LOGOUT/disconnect, then
 * filter here: if targetUserId != null, send only to that connection;
 * otherwise broadcast to everyone (or to everyone in that course, once
 * course subscriptions exist). Until then this is functionally correct
 * but noisier than it needs to be - every client gets every event and
 * decides locally whether it's relevant (see ExamEvent fields).
 */
public class EventBus {

    private final HSTSServer server;

    public EventBus(HSTSServer server) {
        this.server = server;
    }

    public void publish(ExamEvent event) {
        if (server == null || event == null) {
            return;
        }
        server.sendToAllClients(Response.success(Command.EXAM_EVENT, event, event.getMessage(), null));
    }
}

package com.hsts.client.controller;

import com.hsts.client.gui.ApprovalWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.SubjectCoordinator;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.ExamApprovalDecisionData;

import java.util.List;

public class ApprovalClientController implements ResponseHandler {

    private final ServerConnection client;
    private SubjectCoordinator currentCoordinator;
    private ApprovalWindow view;

    public ApprovalClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_PENDING_APPROVAL_EXAMS, this);
        client.registerHandler(Command.APPROVE_EXAM, this);
        client.registerHandler(Command.REJECT_EXAM, this);
        // NEW: if a teacher submits another exam for approval while this
        // screen is open, refresh the queue automatically.
        client.registerHandler(Command.EXAM_EVENT, this);
    }

    public void setCurrentCoordinator(SubjectCoordinator coordinator) {
        this.currentCoordinator = coordinator;
    }

    public void setView(ApprovalWindow view) {
        this.view = view;
    }

    public void refreshPending() {
        client.sendToServer(Command.GET_PENDING_APPROVAL_EXAMS, null);
    }

    public void approve(String examId, String scheduledStart, String scheduledEnd) {
        ExamApprovalDecisionData data = new ExamApprovalDecisionData(examId, currentCoordinator.getId(), null);
        data.setScheduledStart(scheduledStart);
        data.setScheduledEnd(scheduledEnd);
        client.sendToServer(Command.APPROVE_EXAM, data);
    }

    public void reject(String examId, String reason) {
        client.sendToServer(Command.REJECT_EXAM,
                new ExamApprovalDecisionData(examId, currentCoordinator.getId(), reason));
    }

    @Override
    public void handleResponse(Response response) {
        if (view == null) {
            return;
        }
        if (response.getCommand() == Command.EXAM_EVENT) {
            refreshPending();
            return;
        }
        if (!response.isSuccess()) {
            view.showError(response.getMessage());
            return;
        }
        switch (response.getCommand()) {
            case GET_PENDING_APPROVAL_EXAMS -> {
                @SuppressWarnings("unchecked")
                List<Exam> exams = (List<Exam>) response.getPayload();
                view.displayPending(exams);
            }
            case APPROVE_EXAM -> view.onDecisionMade((Exam) response.getPayload(), "Approved.");
            case REJECT_EXAM -> view.onDecisionMade((Exam) response.getPayload(), "Rejected.");
            default -> {
            }
        }
    }
}
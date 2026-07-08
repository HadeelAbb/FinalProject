package com.hsts.client.controller;

import com.hsts.client.gui.GradingWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.ConfirmGradeData;
import com.hsts.shared.net.dto.GetPendingGradingData;

import java.util.List;

public class GradingClientController implements ResponseHandler {

    private final ServerConnection client;
    private Teacher currentTeacher;
    private GradingWindow view;

    public GradingClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_PENDING_GRADING, this);
        client.registerHandler(Command.CONFIRM_GRADE, this);
    }

    public void setCurrentTeacher(Teacher teacher) {
        this.currentTeacher = teacher;
    }

    public void setView(GradingWindow view) {
        this.view = view;
    }

    public void refreshPending() {
        client.sendToServer(Command.GET_PENDING_GRADING, new GetPendingGradingData(currentTeacher.getId()));
    }

    public void confirmGrade(String examAnswerId, Double finalScore, String comment) {
        client.sendToServer(Command.CONFIRM_GRADE,
                new ConfirmGradeData(examAnswerId, currentTeacher.getId(), finalScore, comment));
    }

    @Override
    public void handleResponse(Response response) {
        if (view == null) {
            return;
        }
        if (!response.isSuccess()) {
            view.showError(response.getMessage());
            return;
        }
        switch (response.getCommand()) {
            case GET_PENDING_GRADING -> {
                @SuppressWarnings("unchecked")
                List<ExamAnswer> pending = (List<ExamAnswer>) response.getPayload();
                view.displayPending(pending);
            }
            case CONFIRM_GRADE -> {
                view.onGradeConfirmed((ExamAnswer) response.getPayload());
                refreshPending();
            }
            default -> {
            }
        }
    }
}

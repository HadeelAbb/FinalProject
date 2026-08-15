package com.hsts.client.controller;

import com.hsts.client.gui.GradingWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.ConfirmGradeData;
import com.hsts.shared.net.dto.GetExamDetailData;
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
        client.registerHandler(Command.GET_EXAM_DETAIL, this);
        client.registerHandler(Command.EXAM_EVENT, this);
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

    public void loadExamDetail(String examId) {
        client.sendToServer(Command.GET_EXAM_DETAIL, new GetExamDetailData(examId));
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
        // A live push (someone else submitted an exam) - just refresh, nothing else to do with it.
        if (response.getCommand() == Command.EXAM_EVENT) {
            refreshPending();
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
            case GET_EXAM_DETAIL -> view.displayExamDetail((Exam) response.getPayload());
            case CONFIRM_GRADE -> {
                view.onGradeConfirmed((ExamAnswer) response.getPayload());
                refreshPending();
            }
            default -> {
            }
        }
    }
}
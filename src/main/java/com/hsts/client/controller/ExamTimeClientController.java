package com.hsts.client.controller;

import com.hsts.client.gui.ExamTimeWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.ExtendExamTimeData;
import com.hsts.shared.net.dto.GetMyExamsData;

import java.util.List;

public class ExamTimeClientController implements ResponseHandler {

    private final ServerConnection client;
    private Teacher currentTeacher;
    private ExamTimeWindow view;

    public ExamTimeClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_MY_EXAMS, this);
        client.registerHandler(Command.EXTEND_EXAM_TIME, this);
    }

    public void setCurrentTeacher(Teacher teacher) {
        this.currentTeacher = teacher;
    }

    public void setView(ExamTimeWindow view) {
        this.view = view;
    }

    public void refreshMyExams() {
        client.sendToServer(Command.GET_MY_EXAMS, new GetMyExamsData(currentTeacher.getId()));
    }

    public void extend(String examId, int additionalMinutes) {
        client.sendToServer(Command.EXTEND_EXAM_TIME,
                new ExtendExamTimeData(examId, currentTeacher.getId(), additionalMinutes));
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
            case GET_MY_EXAMS -> {
                @SuppressWarnings("unchecked")
                List<Exam> exams = (List<Exam>) response.getPayload();
                view.displayExams(exams);
            }
            case EXTEND_EXAM_TIME -> {
                view.onExtended((Exam) response.getPayload());
                refreshMyExams();
            }
            default -> {
            }
        }
    }
}

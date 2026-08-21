package com.hsts.client.controller;

import com.hsts.client.gui.TeacherResultsWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.GetExamResultsData;
import com.hsts.shared.net.dto.GetMyExamsData;

import java.util.List;

public class TeacherResultsClientController implements ResponseHandler {

    private final ServerConnection client;
    private Teacher currentTeacher;
    private TeacherResultsWindow view;

    public TeacherResultsClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_MY_EXAMS, this);
        client.registerHandler(Command.GET_EXAM_RESULTS, this);
    }

    public void setCurrentTeacher(Teacher teacher) {
        this.currentTeacher = teacher;
    }

    public void setView(TeacherResultsWindow view) {
        this.view = view;
    }

    public void refreshMyExams() {
        client.sendToServer(Command.GET_MY_EXAMS, new GetMyExamsData(currentTeacher.getId()));
    }

    public void loadExamResults(String examId) {
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        client.sendToServer(Command.GET_EXAM_RESULTS, new GetExamResultsData(examId, teacherId));
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
            case GET_EXAM_RESULTS -> {
                @SuppressWarnings("unchecked")
                List<ExamAnswer> results = (List<ExamAnswer>) response.getPayload();
                view.displayResults(results);
            }
            default -> {
            }
        }
    }
}

package com.hsts.client.controller;

import com.hsts.client.gui.ResultsWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Student;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.GetExamAnswerCopyData;
import com.hsts.shared.net.dto.GetMyResultsData;

import java.util.List;

public class ResultsClientController implements ResponseHandler {

    private final ServerConnection client;
    private Student currentStudent;
    private ResultsWindow view;

    public ResultsClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_MY_RESULTS, this);
        client.registerHandler(Command.GET_EXAM_ANSWER_COPY, this);
    }

    public void setCurrentStudent(Student student) {
        this.currentStudent = student;
    }

    public void setView(ResultsWindow view) {
        this.view = view;
    }

    public void refreshResults() {
        client.sendToServer(Command.GET_MY_RESULTS, new GetMyResultsData(currentStudent.getId()));
    }

    public void viewCopy(String examAnswerId) {
        client.sendToServer(Command.GET_EXAM_ANSWER_COPY,
                new GetExamAnswerCopyData(examAnswerId, currentStudent.getId()));
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
            case GET_MY_RESULTS -> {
                @SuppressWarnings("unchecked")
                List<ExamAnswer> results = (List<ExamAnswer>) response.getPayload();
                view.displayResults(results);
            }
            case GET_EXAM_ANSWER_COPY -> {
                Object[] pair = (Object[]) response.getPayload();
                view.displayCopy((Exam) pair[0], (ExamAnswer) pair[1]);
            }
            default -> {
            }
        }
    }
}

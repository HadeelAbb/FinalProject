package com.hsts.client.controller;

import com.hsts.client.gui.PrincipalOverviewWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.ExamStats;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.GetExamStatsData;

import java.util.List;

public class PrincipalClientController implements ResponseHandler {

    private final ServerConnection client;
    private PrincipalOverviewWindow view;

    public PrincipalClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_ALL_EXAMS, this);
        client.registerHandler(Command.GET_ALL_RESULTS, this);
        client.registerHandler(Command.GET_EXAM_STATS, this);
    }

    public void setView(PrincipalOverviewWindow view) {
        this.view = view;
    }

    public void loadAllExams() {
        client.sendToServer(Command.GET_ALL_EXAMS, null);
    }

    public void loadAllResults() {
        client.sendToServer(Command.GET_ALL_RESULTS, null);
    }

    public void loadExamStats(String examId) {
        client.sendToServer(Command.GET_EXAM_STATS, new GetExamStatsData(examId));
    }

    @Override
    public void handleResponse(Response response) {
        if (view == null) {
            return;
        }
        if (!response.isSuccess()) {
            if (response.getCommand() == Command.GET_EXAM_STATS) {
                view.displayStatsUnavailable(response.getMessage());
            } else {
                view.showError(response.getMessage());
            }
            return;
        }
        switch (response.getCommand()) {
            case GET_ALL_EXAMS -> {
                @SuppressWarnings("unchecked")
                List<Exam> exams = (List<Exam>) response.getPayload();
                view.displayAllExams(exams);
            }
            case GET_ALL_RESULTS -> {
                @SuppressWarnings("unchecked")
                List<ExamAnswer> results = (List<ExamAnswer>) response.getPayload();
                view.displayAllResults(results);
            }
            case GET_EXAM_STATS -> view.displayStats((ExamStats) response.getPayload());
            default -> {
            }
        }
    }
}
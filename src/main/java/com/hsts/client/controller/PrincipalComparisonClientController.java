package com.hsts.client.controller;

import com.hsts.client.gui.PrincipalComparisonWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.PrincipalComparisonReport;
import com.hsts.shared.model.PrincipalReportType;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.PrincipalComparisonReportData;

import java.util.List;

public class PrincipalComparisonClientController implements ResponseHandler {

    private final ServerConnection client;
    private PrincipalComparisonWindow view;

    public PrincipalComparisonClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_ALL_EXAMS, this);
        client.registerHandler(Command.GET_ALL_RESULTS, this);
        client.registerHandler(Command.GET_PRINCIPAL_COMPARISON_REPORT, this);
    }

    public void setView(PrincipalComparisonWindow view) {
        this.view = view;
    }

    public void loadSelectors() {
        client.sendToServer(Command.GET_ALL_EXAMS, null);
        client.sendToServer(Command.GET_ALL_RESULTS, null);
    }

    public void generateReport(PrincipalReportType type, String filterValue) {
        client.sendToServer(Command.GET_PRINCIPAL_COMPARISON_REPORT,
                new PrincipalComparisonReportData(type, filterValue));
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
            case GET_ALL_EXAMS -> {
                @SuppressWarnings("unchecked")
                List<Exam> exams = (List<Exam>) response.getPayload();
                view.displayExams(exams);
            }
            case GET_ALL_RESULTS -> {
                @SuppressWarnings("unchecked")
                List<ExamAnswer> results = (List<ExamAnswer>) response.getPayload();
                view.displayConfirmedResults(results);
            }
            case GET_PRINCIPAL_COMPARISON_REPORT ->
                    view.displayReport((PrincipalComparisonReport) response.getPayload());
            default -> {
            }
        }
    }
}

package com.hsts.client.controller;

import com.hsts.client.gui.ExamTimeWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamExecution;
import com.hsts.shared.model.ExecutionStats;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.CreateExamExecutionData;
import com.hsts.shared.net.dto.ExtendExamTimeData;
import com.hsts.shared.net.dto.GetExamExecutionsData;
import com.hsts.shared.net.dto.GetExecutionStatsData;
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
        client.registerHandler(Command.CREATE_EXAM_EXECUTION, this);
        client.registerHandler(Command.GET_EXAM_EXECUTIONS, this);
        client.registerHandler(Command.GET_EXECUTION_STATS, this);
        client.registerHandler(Command.EXAM_EVENT, this);
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

    /** SUC-17: extends time for a SPECIFIC execution - applies to it as a whole, not just currently-connected students. */
    public void extend(String examId, String executionId, int additionalMinutes) {
        client.sendToServer(Command.EXTEND_EXAM_TIME,
                new ExtendExamTimeData(examId, executionId, currentTeacher.getId(), additionalMinutes));
    }

    /** SUC 2.2: opens a NEW sitting of an already-approved exam, with teacher-chosen code and window. */
    public void createExecution(String examId, String scheduledStart, String scheduledEnd, String executionCode) {
        client.sendToServer(Command.CREATE_EXAM_EXECUTION,
                new CreateExamExecutionData(examId, currentTeacher.getId(), scheduledStart, scheduledEnd, executionCode));
    }

    public void loadExecutions(String examId) {
        client.sendToServer(Command.GET_EXAM_EXECUTIONS, new GetExamExecutionsData(examId));
    }

    public void loadExecutionStats(String executionId) {
        client.sendToServer(Command.GET_EXECUTION_STATS, new GetExecutionStatsData(executionId));
    }

    @Override
    public void handleResponse(Response response) {
        if (view == null) {
            return;
        }
        // A live push (an exam was approved/rejected, or someone opened a new
        // execution somewhere) - refresh the exam list; the window itself
        // re-loads executions for whatever exam is currently selected.
        if (response.getCommand() == Command.EXAM_EVENT) {
            refreshMyExams();
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
                view.onExtended((ExamExecution) response.getPayload(), response.getMessage());
            }
            case CREATE_EXAM_EXECUTION -> {
                view.onExecutionCreated((ExamExecution) response.getPayload());
            }
            case GET_EXAM_EXECUTIONS -> {
                @SuppressWarnings("unchecked")
                List<ExamExecution> executions = (List<ExamExecution>) response.getPayload();
                view.displayExecutions(executions);
            }
            case GET_EXECUTION_STATS -> view.displayExecutionStats((ExecutionStats) response.getPayload());
            default -> {
            }
        }
    }
}
package com.hsts.client.controller;

import com.hsts.client.gui.ExamTakingWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Student;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.ExamEvent;
import com.hsts.shared.net.EventType;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.GetAvailableExamsData;
import com.hsts.shared.net.dto.StartExamData;
import com.hsts.shared.net.dto.SubmitExamData;

import java.util.List;
import java.util.Map;

public class ExamTakingClientController implements ResponseHandler {

    private final ServerConnection client;
    private Student currentStudent;
    private Exam currentExam;
    /** SUC 2.2: remembered so it can be resent at submit time to resolve which execution this belongs to. */
    private String currentExecutionCode;
    private ExamTakingWindow view;

    public ExamTakingClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_AVAILABLE_EXAMS, this);
        client.registerHandler(Command.START_EXAM, this);
        client.registerHandler(Command.SUBMIT_EXAM, this);
        client.registerHandler(Command.EXAM_EVENT, this);
    }

    public void setCurrentStudent(Student student) {
        this.currentStudent = student;
    }

    public void setView(ExamTakingWindow view) {
        this.view = view;
    }

    public void loadAvailableExams() {
        client.sendToServer(Command.GET_AVAILABLE_EXAMS, new GetAvailableExamsData(currentStudent.getId()));
    }

    public void startExam(String examId, String executionCode) {
        this.currentExecutionCode = executionCode;
        client.sendToServer(Command.START_EXAM,
                new StartExamData(examId, currentStudent.getId(), executionCode));
    }

    public void submitExam(Map<String, String> selectedAnswers, boolean autoSubmitted) {
        if (currentExam == null) {
            return;
        }
        SubmitExamData data = new SubmitExamData(currentExam.getExamId(), currentStudent.getId(), selectedAnswers, autoSubmitted);
        data.setExecutionCode(currentExecutionCode);
        client.sendToServer(Command.SUBMIT_EXAM, data);
    }

    @Override
    public void handleResponse(Response response) {
        if (view == null) {
            return;
        }
        // EXAM_EVENT is a server-initiated push, not a reply to something we sent -
        // it can arrive at any time, including when there's nothing to do with it.
        if (response.getCommand() == Command.EXAM_EVENT) {
            if (response.getPayload() instanceof ExamEvent event) {
                if (event.getType() == EventType.EXAM_TIME_EXTENDED
                        && currentExam != null
                        && currentExam.getExamId().equals(event.getExamId())) {
                    view.onTimeExtended(event.getExtraMinutes(), event.getMessage());
                } else if ((event.getType() == EventType.EXECUTION_CREATED
                        || event.getType() == EventType.EXAM_APPROVED)
                        && currentExam == null) {
                    // A new execution opened somewhere, or an exam was approved - the
                    // available-exams list might have changed. Only refresh if we're
                    // not mid-exam right now, so this never disrupts an active attempt.
                    loadAvailableExams();
                }
            }
            return;
        }
        if (!response.isSuccess()) {
            view.showError(response.getMessage());
            return;
        }
        switch (response.getCommand()) {
            case GET_AVAILABLE_EXAMS -> {
                @SuppressWarnings("unchecked")
                List<Exam> exams = (List<Exam>) response.getPayload();
                view.displayAvailableExams(exams);
            }
            case START_EXAM -> {
                currentExam = (Exam) response.getPayload();
                view.onExamStarted(currentExam);
            }
            case SUBMIT_EXAM -> view.onExamSubmitted((ExamAnswer) response.getPayload());
            default -> {
            }
        }
    }
}
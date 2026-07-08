package com.hsts.client.controller;

import com.hsts.client.gui.ExamTakingWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.ExamAnswer;
import com.hsts.shared.model.Student;
import com.hsts.shared.net.Command;
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
    private ExamTakingWindow view;

    public ExamTakingClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_AVAILABLE_EXAMS, this);
        client.registerHandler(Command.START_EXAM, this);
        client.registerHandler(Command.SUBMIT_EXAM, this);
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

    public void startExam(String examId) {
        client.sendToServer(Command.START_EXAM, new StartExamData(examId, currentStudent.getId()));
    }

    public void submitExam(Map<String, String> selectedAnswers, boolean autoSubmitted) {
        if (currentExam == null) {
            return;
        }
        client.sendToServer(Command.SUBMIT_EXAM,
                new SubmitExamData(currentExam.getExamId(), currentStudent.getId(), selectedAnswers, autoSubmitted));
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

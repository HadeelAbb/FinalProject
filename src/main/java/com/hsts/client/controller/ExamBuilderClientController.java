package com.hsts.client.controller;

import com.hsts.client.gui.ExamBuilderWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Exam;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.CreateExamAutoData;
import com.hsts.shared.net.dto.CreateExamManualData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import com.hsts.shared.net.dto.SubmitExamForApprovalData;

import java.util.List;

public class ExamBuilderClientController implements ResponseHandler {

    private final ServerConnection client;
    private Teacher currentTeacher;
    private Exam currentDraft;
    private ExamBuilderWindow view;

    public ExamBuilderClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.CREATE_EXAM_MANUAL, this);
        client.registerHandler(Command.CREATE_EXAM_AUTO, this);
        client.registerHandler(Command.SUBMIT_EXAM_FOR_APPROVAL, this);
        client.registerHandler(Command.SEARCH_QUESTIONS, this);
    }

    public void searchQuestionsForCourse(String courseId) {
        client.sendToServer(Command.SEARCH_QUESTIONS, new SearchQuestionsData(courseId, null, null));
    }

    public void setView(ExamBuilderWindow view) {
        this.view = view;
    }

    public void setCurrentTeacher(Teacher teacher) {
        this.currentTeacher = teacher;
    }

    public void createManual(String courseId, String title, String instructions,
                              List<String> questionIds, int durationMinutes) {
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        client.sendToServer(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacherId, courseId, title, instructions, questionIds, durationMinutes));
    }

    public void createAuto(String courseId, String title, String instructions, String topic,
                            Difficulty difficulty, int numberOfQuestions, int durationMinutes) {
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        client.sendToServer(Command.CREATE_EXAM_AUTO, new CreateExamAutoData(teacherId, courseId, title,
                instructions, topic, difficulty, numberOfQuestions, durationMinutes));
    }

    public void submitForApproval() {
        if (currentDraft == null) {
            return;
        }
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        client.sendToServer(Command.SUBMIT_EXAM_FOR_APPROVAL,
                new SubmitExamForApprovalData(currentDraft.getExamId(), teacherId));
    }

    public Exam getCurrentDraft() {
        return currentDraft;
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
            case CREATE_EXAM_MANUAL, CREATE_EXAM_AUTO -> {
                currentDraft = (Exam) response.getPayload();
                view.onExamCreated(currentDraft);
            }
            case SUBMIT_EXAM_FOR_APPROVAL -> {
                currentDraft = (Exam) response.getPayload();
                view.onSubmittedForApproval(currentDraft);
            }
            case SEARCH_QUESTIONS -> {
                @SuppressWarnings("unchecked")
                List<Question> questions = (List<Question>) response.getPayload();
                view.displayQuestionBank(questions);
            }
            default -> {
            }
        }
    }
}

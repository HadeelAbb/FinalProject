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
import com.hsts.shared.net.dto.CreateExamVersionData;
import com.hsts.shared.net.dto.GetMyExamsData;
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
        client.registerHandler(Command.CREATE_EXAM_VERSION, this);
        client.registerHandler(Command.GET_MY_EXAMS, this);
        client.registerHandler(Command.SUBMIT_EXAM_FOR_APPROVAL, this);
        client.registerHandler(Command.SEARCH_QUESTIONS, this);
        client.registerHandler(Command.EXAM_EVENT, this);
    }

    public void searchQuestionsForCourse(String courseId) {
        SearchQuestionsData data = new SearchQuestionsData(courseId, null, null);
        data.setLatestOnly(true);
        client.sendToServer(Command.SEARCH_QUESTIONS, data);
    }

    public void setView(ExamBuilderWindow view) {
        this.view = view;
    }

    public void setCurrentTeacher(Teacher teacher) {
        this.currentTeacher = teacher;
    }

    public void refreshMyExams() {
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        client.sendToServer(Command.GET_MY_EXAMS, new GetMyExamsData(teacherId));
    }

    public void createManual(String courseId, String title, String instructions, String teacherNotes,
                             List<String> questionIds, java.util.Map<String, Integer> questionPoints, int durationMinutes) {
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        client.sendToServer(Command.CREATE_EXAM_MANUAL,
                new CreateExamManualData(teacherId, courseId, title, instructions, teacherNotes,
                        questionIds, questionPoints, durationMinutes));
    }

    public void createVersion(String sourceExamId, String title, String instructions, String teacherNotes,
                              List<String> questionIds, java.util.Map<String, Integer> questionPoints,
                              int durationMinutes) {
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        client.sendToServer(Command.CREATE_EXAM_VERSION,
                new CreateExamVersionData(sourceExamId, teacherId, title, instructions, teacherNotes,
                        questionIds, questionPoints, durationMinutes));
    }

    public void createAuto(String courseId, String title, String instructions, String teacherNotes, String topic,
                           Difficulty difficulty, int numberOfQuestions, int durationMinutes) {
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        client.sendToServer(Command.CREATE_EXAM_AUTO, new CreateExamAutoData(teacherId, courseId, title,
                instructions, teacherNotes, topic, difficulty, numberOfQuestions, durationMinutes));
    }

    /**
     * The exam currently loaded in the builder. Selecting a persisted current
     * DRAFT must set this so Review / Submit use that same physical examId.
     */
    public void selectExistingExam(Exam exam) {
        this.currentDraft = exam;
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
        // A live push - only worth reacting to if it's about the exam we're currently viewing.
        if (response.getCommand() == Command.EXAM_EVENT) {
            if (response.getPayload() instanceof com.hsts.shared.net.ExamEvent event
                    && currentDraft != null
                    && currentDraft.getExamId().equals(event.getExamId())
                    && (event.getType() == com.hsts.shared.net.EventType.EXAM_APPROVED
                    || event.getType() == com.hsts.shared.net.EventType.EXAM_REJECTED)) {
                view.onStatusChanged(event.getMessage());
            }
            return;
        }
        if (!response.isSuccess()) {
            view.showError(response.getMessage());
            return;
        }
        switch (response.getCommand()) {
            case CREATE_EXAM_MANUAL, CREATE_EXAM_AUTO, CREATE_EXAM_VERSION -> {
                currentDraft = (Exam) response.getPayload();
                view.onExamCreated(currentDraft);
            }
            case GET_MY_EXAMS -> {
                @SuppressWarnings("unchecked")
                List<Exam> exams = (List<Exam>) response.getPayload();
                view.displayMyExams(exams);
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
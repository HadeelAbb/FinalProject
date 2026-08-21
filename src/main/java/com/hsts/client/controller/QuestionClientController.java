package com.hsts.client.controller;

import com.hsts.client.gui.QuestionManagementWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionAnswer;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.DeleteQuestionData;
import com.hsts.shared.net.dto.EditQuestionData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;

import java.util.List;

public class QuestionClientController implements ResponseHandler {

    private final ServerConnection client;
    private Question currentQuestion;
    private QuestionManagementWindow view;
    private Teacher currentTeacher;

    public QuestionClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.SEARCH_QUESTIONS, this);
        client.registerHandler(Command.CREATE_QUESTION, this);
        client.registerHandler(Command.EDIT_QUESTION, this);
        client.registerHandler(Command.DELETE_QUESTION, this);
        // NEW: also listen for the server's broadcast notification that
        // another client changed the question list, so this client can
        // refresh automatically.
        client.registerHandler(Command.QUESTIONS_CHANGED, this);
    }

    public void setView(QuestionManagementWindow view) {
        this.view = view;
    }

    public void setCurrentTeacher(Teacher teacher) {
        this.currentTeacher = teacher;
    }

    public Teacher getCurrentTeacher() {
        return currentTeacher;
    }

    public void createQuestion(String text, String instructions, Difficulty difficulty, String topic,
                               String imagePath, byte[] imageData, String courseId, List<QuestionAnswer> answers) {
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        CreateQuestionData data = new CreateQuestionData(text, instructions, difficulty, topic, imagePath,
                courseId, teacherId, answers);
        data.setImageData(imageData);
        client.sendToServer(Command.CREATE_QUESTION, data);
    }

    public void editQuestion(Question question) {
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        EditQuestionData data = new EditQuestionData(question.getQuestionId(), question.getText(),
                question.getInstructions(), question.getDifficulty(), question.getTopic(),
                question.getImagePath(), teacherId, question.getAnswers());
        data.setImageData(question.getImageData());
        client.sendToServer(Command.EDIT_QUESTION, data);
    }

    public void deleteQuestion(Question question) {
        String teacherId = currentTeacher != null ? currentTeacher.getId() : null;
        client.sendToServer(Command.DELETE_QUESTION,
                new DeleteQuestionData(question.getQuestionId(), teacherId));
    }

    public void searchQuestions(String topic, Difficulty difficulty, String courseId) {
        client.sendToServer(Command.SEARCH_QUESTIONS, new SearchQuestionsData(courseId, topic, difficulty));
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
            case SEARCH_QUESTIONS -> {
                @SuppressWarnings("unchecked")
                List<Question> questions = (List<Question>) response.getPayload();
                view.displayQuestions(questions);
            }
            case CREATE_QUESTION, EDIT_QUESTION -> {
                currentQuestion = (Question) response.getPayload();
                view.onQuestionSaved(currentQuestion);
            }
            case DELETE_QUESTION -> view.onQuestionDeleted();
            // NEW: another client created/edited/deleted a question.
            // Refresh this client's list too, so changes appear on every
            // open client instead of only the one that made the change.
            case QUESTIONS_CHANGED -> view.refreshFromServerNotification();
            default -> {
            }
        }
    }

    public Question getCurrentQuestion() {
        return currentQuestion;
    }
}
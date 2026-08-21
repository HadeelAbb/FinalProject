package com.hsts.client.controller;

import com.hsts.client.gui.PrincipalQuestionBankWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.Difficulty;
import com.hsts.shared.model.Question;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.SearchQuestionsData;

import java.util.List;

public class PrincipalQuestionBankClientController implements ResponseHandler {

    private final ServerConnection client;
    private PrincipalQuestionBankWindow view;

    public PrincipalQuestionBankClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_ALL_QUESTIONS, this);
    }

    public void setView(PrincipalQuestionBankWindow view) {
        this.view = view;
    }

    public void search(String courseId, String topic, Difficulty difficulty) {
        client.sendToServer(Command.GET_ALL_QUESTIONS, new SearchQuestionsData(courseId, topic, difficulty));
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
        if (response.getCommand() == Command.GET_ALL_QUESTIONS) {
            @SuppressWarnings("unchecked")
            List<Question> questions = (List<Question>) response.getPayload();
            view.displayQuestions(questions);
        }
    }
}

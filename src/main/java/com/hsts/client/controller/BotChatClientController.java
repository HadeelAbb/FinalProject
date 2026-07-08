package com.hsts.client.controller;

import com.hsts.client.gui.BotChatWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.BotInteraction;
import com.hsts.shared.model.Student;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.AskBotQuestionData;
import com.hsts.shared.net.dto.GetBotHistoryData;

import java.util.List;

public class BotChatClientController implements ResponseHandler {

    private final ServerConnection client;
    private Student currentStudent;
    private BotChatWindow view;

    public BotChatClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.ASK_BOT_QUESTION, this);
        client.registerHandler(Command.GET_BOT_HISTORY, this);
    }

    public void setCurrentStudent(Student student) {
        this.currentStudent = student;
    }

    public void setView(BotChatWindow view) {
        this.view = view;
    }

    public void ask(String courseId, String question) {
        client.sendToServer(Command.ASK_BOT_QUESTION,
                new AskBotQuestionData(currentStudent.getId(), courseId, question));
    }

    public void refreshHistory() {
        client.sendToServer(Command.GET_BOT_HISTORY, new GetBotHistoryData(currentStudent.getId()));
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
            case ASK_BOT_QUESTION -> {
                view.onAnswerReceived((BotInteraction) response.getPayload());
                refreshHistory();
            }
            case GET_BOT_HISTORY -> {
                @SuppressWarnings("unchecked")
                List<BotInteraction> history = (List<BotInteraction>) response.getPayload();
                view.displayHistory(history);
            }
            default -> {
            }
        }
    }
}

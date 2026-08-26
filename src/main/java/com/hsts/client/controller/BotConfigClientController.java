package com.hsts.client.controller;

import com.hsts.client.gui.BotConfigWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.CourseBotConfig;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.GetBotConfigData;
import com.hsts.shared.net.dto.UpdateBotConfigData;

public class BotConfigClientController implements ResponseHandler {

    private final ServerConnection client;
    private BotConfigWindow view;

    public BotConfigClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_BOT_CONFIG, this);
        client.registerHandler(Command.UPDATE_BOT_CONFIG, this);
    }

    public void setView(BotConfigWindow view) {
        this.view = view;
    }

    public void loadBotConfig(String courseId) {
        client.sendToServer(Command.GET_BOT_CONFIG, new GetBotConfigData(courseId));
    }

    public void saveBotConfig(String courseId, String botName, String knowledgeSources, boolean active) {
        UpdateBotConfigData payload = new UpdateBotConfigData(courseId, null, botName, knowledgeSources, active);
        client.sendToServer(Command.UPDATE_BOT_CONFIG, payload);
    }

    @Override
    public void handleResponse(Response response) {
        if (view == null) {
            return;
        }

        if (!response.isSuccess()) {
            view.showError(response.getMessage() != null ? response.getMessage() : "Request failed.");
            return;
        }

        switch (response.getCommand()) {
            case GET_BOT_CONFIG -> {
                if (response.getPayload() instanceof CourseBotConfig config) {
                    view.onConfigLoaded(config);
                }
            }
            case UPDATE_BOT_CONFIG -> {
                if (response.getPayload() instanceof CourseBotConfig config) {
                    view.onConfigSaved(config);
                }
            }
            default -> {
            }
        }
    }
}
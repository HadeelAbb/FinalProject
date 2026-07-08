package com.hsts.client.controller;

import com.hsts.client.gui.BotStatsWindow;
import com.hsts.client.network.ResponseHandler;
import com.hsts.client.network.ServerConnection;
import com.hsts.shared.model.BotUsageStats;
import com.hsts.shared.model.Teacher;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.GetBotUsageStatsData;

import java.util.List;

public class BotStatsClientController implements ResponseHandler {

    private final ServerConnection client;
    private Teacher currentTeacher;
    private BotStatsWindow view;

    public BotStatsClientController(ServerConnection client) {
        this.client = client;
        client.registerHandler(Command.GET_BOT_USAGE_STATS, this);
    }

    public void setCurrentTeacher(Teacher teacher) {
        this.currentTeacher = teacher;
    }

    public void setView(BotStatsWindow view) {
        this.view = view;
    }

    public void refreshStats() {
        client.sendToServer(Command.GET_BOT_USAGE_STATS, new GetBotUsageStatsData(currentTeacher.getId()));
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
        if (response.getCommand() == Command.GET_BOT_USAGE_STATS) {
            @SuppressWarnings("unchecked")
            List<BotUsageStats> stats = (List<BotUsageStats>) response.getPayload();
            view.displayStats(stats);
        }
    }
}

package com.hsts.shared.net.dto;

import java.io.Serializable;

/** Section 4: request the started/finished/timed-out counts for one specific execution. */
public class GetExecutionStatsData implements Serializable {
    private String executionId;

    public GetExecutionStatsData() {
    }

    public GetExecutionStatsData(String executionId) {
        this.executionId = executionId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
}
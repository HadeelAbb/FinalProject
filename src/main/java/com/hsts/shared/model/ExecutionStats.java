package com.hsts.shared.model;

import java.io.Serializable;

/**
 * Section 4: for one exam execution - how many students started (which,
 * given this client always auto-submits at time-up, equals total
 * submissions), how many finished on their own, and how many ran out
 * of time ("didn't manage").
 */
public class ExecutionStats implements Serializable {

    private String executionId;
    private int totalStarted;
    private int finishedThemselves;
    private int didntManage;

    public ExecutionStats() {
    }

    public ExecutionStats(String executionId, int totalStarted, int finishedThemselves, int didntManage) {
        this.executionId = executionId;
        this.totalStarted = totalStarted;
        this.finishedThemselves = finishedThemselves;
        this.didntManage = didntManage;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public int getTotalStarted() {
        return totalStarted;
    }

    public void setTotalStarted(int totalStarted) {
        this.totalStarted = totalStarted;
    }

    public int getFinishedThemselves() {
        return finishedThemselves;
    }

    public void setFinishedThemselves(int finishedThemselves) {
        this.finishedThemselves = finishedThemselves;
    }

    public int getDidntManage() {
        return didntManage;
    }

    public void setDidntManage(int didntManage) {
        this.didntManage = didntManage;
    }
}
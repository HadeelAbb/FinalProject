//Solves Requirement 13 & 14 (Spec 6.1 / Scenario 13):
// The project spec mandates that a teacher can create a bot for their course,
// activate/deactivate it,
// and define knowledge sources.
//Enables Multi-Teacher Collaboration:
// If two teachers teach CS101, both can load and edit the same course bot's knowledge sources.
// The last_updated_by field tracks who made the latest change.

package com.hsts.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class CourseBotConfig implements Serializable {
    private String courseId;
    private String botName;
    private String knowledgeSources;
    private boolean active = true;
    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;

    public CourseBotConfig() {
    }

    public CourseBotConfig(String courseId, String botName, String knowledgeSources, boolean active, String lastUpdatedBy) {
        this.courseId = courseId;
        this.botName = botName;
        this.knowledgeSources = knowledgeSources;
        this.active = active;
        this.lastUpdatedBy = lastUpdatedBy;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getKnowledgeSources() {
        return knowledgeSources;
    }

    public void setKnowledgeSources(String knowledgeSources) {
        this.knowledgeSources = knowledgeSources;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
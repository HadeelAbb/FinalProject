package com.hsts.shared.net;

import java.io.Serializable;

/**
 * Payload carried by a Command.EXAM_EVENT broadcast. Generalizes the same
 * idea as QUESTIONS_CHANGED but for the exam lifecycle: any client whose
 * open screen cares about this examId (or courseId) should refresh.
 *
 * targetUserId is optional - if set, the event is meant for one specific
 * user (e.g. "your exam was approved"); if null, it's relevant to anyone
 * watching that course/exam (e.g. a coordinator's approval queue).
 */
public class ExamEvent implements Serializable {
    private EventType type;
    private String examId;
    private String courseId;
    private String targetUserId;
    private String message;
    private int extraMinutes;

    public ExamEvent() {
    }

    public ExamEvent(EventType type, String examId, String courseId, String targetUserId, String message) {
        this.type = type;
        this.examId = examId;
        this.courseId = courseId;
        this.targetUserId = targetUserId;
        this.message = message;
    }

    public int getExtraMinutes() {
        return extraMinutes;
    }

    public void setExtraMinutes(int extraMinutes) {
        this.extraMinutes = extraMinutes;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
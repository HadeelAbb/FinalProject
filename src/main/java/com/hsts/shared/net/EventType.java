package com.hsts.shared.net;

import java.io.Serializable;

/**
 * Kinds of server-initiated broadcast events, published through the
 * server-side EventBus (Partner 2) and delivered to clients as a
 * Command.EXAM_EVENT response carrying an ExamEvent payload.
 */
public enum EventType implements Serializable {
    EXAM_SUBMITTED_FOR_APPROVAL,
    EXAM_APPROVED,
    EXAM_REJECTED,
    EXAM_GRADED,
    EXAM_TIME_EXTENDED,
    EXECUTION_CREATED,
    EXAM_ANSWER_SUBMITTED
}
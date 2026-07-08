package com.hsts.shared.net;

public enum Command {
    LOGIN,
    LOGOUT,
    SEARCH_QUESTIONS,
    CREATE_QUESTION,
    EDIT_QUESTION,
    DELETE_QUESTION,
    // NEW: server broadcasts this to every connected client whenever any
    // client successfully creates, edits, or deletes a question, so every
    // open GUI can refresh its list automatically instead of only the
    // client that made the change seeing the update.
    QUESTIONS_CHANGED,

    // ===== Exam building (SUC-2 manual, SUC-3 automatic) =====
    CREATE_EXAM_MANUAL,
    CREATE_EXAM_AUTO,
    GET_MY_EXAMS,

    // ===== Exam approval (SUC-4) =====
    SUBMIT_EXAM_FOR_APPROVAL,
    GET_PENDING_APPROVAL_EXAMS,
    APPROVE_EXAM,
    REJECT_EXAM,

    // ===== Taking an exam (SUC-6) =====
    GET_AVAILABLE_EXAMS,
    START_EXAM,
    SUBMIT_EXAM,

    // ===== Grading (SUC-7 automatic, SUC-8 teacher confirmation) =====
    GET_PENDING_GRADING,
    CONFIRM_GRADE,

    // ===== Viewing results (SUC-10) =====
    GET_MY_RESULTS,
    GET_EXAM_ANSWER_COPY,

    // ===== Changing exam time mid-execution (SUC-17) =====
    EXTEND_EXAM_TIME,

    // ===== Study bot (SUC-13 teacher stats, SUC-14 ask, SUC-15 history) =====
    ASK_BOT_QUESTION,
    GET_BOT_HISTORY,
    GET_BOT_USAGE_STATS,

    // NEW: generic broadcast event, published by the server-side EventBus
    // whenever exam status, grading, or timing changes in a way that
    // affects other connected clients (e.g. a coordinator approves an
    // exam -> the teacher's screen should update; a teacher extends time
    // -> the student's screen should update). Payload is an ExamEvent.
    EXAM_EVENT
}
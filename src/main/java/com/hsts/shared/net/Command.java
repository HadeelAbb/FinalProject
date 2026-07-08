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
    QUESTIONS_CHANGED
}
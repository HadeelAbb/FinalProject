package com.hsts.shared.model;

import java.io.Serializable;

/**
 * Client-side copy of Partner 1's QuestionAnswer entity (PDOM).
 * Will be replaced/aligned with Partner 1's real class once delivered -
 * field names and method names below are the agreed contract, keep them
 * stable so the GUI code doesn't need to change at integration time.
 */
public class QuestionAnswer implements Serializable {

    private String text;
    private boolean correct;

    public QuestionAnswer() {
    }

    public QuestionAnswer(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    @Override
    public String toString() {
        return text + (correct ? " (correct)" : "");
    }
}

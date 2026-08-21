package com.hsts.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side copy of Partner 1's Question entity (PDOM).
 * questionId format matches the SQL schema: 3-digit sequence + 2-digit
 * course code (e.g. "00111" = question 001 in course 11).
 */
public class Question implements Serializable {

    private String questionId;
    private String text;
    private String instructions;
    private Difficulty difficulty;
    private String topic;
    /** Original filename only (never a teacher-local filesystem path). */
    private String imagePath;
    /** PNG/JPG bytes persisted in MySQL and carried over OCSF. Not a JavaFX Image. */
    private byte[] imageData;
    private String courseId;
    /**
     * Physical question_id of version 1 in this lineage. Exam rows keep their
     * own physical question_id; this only groups versions in the bank.
     */
    private String rootQuestionId;
    private int versionNumber = 1;
    private boolean latest = true;
    private List<QuestionAnswer> answers = new ArrayList<>();
    /**
     * Points for this question inside a specific exam (exam_questions.points).
     * Not a global property of the question bank.
     */
    private int points;

    public Question() {
    }

    public Question(String questionId, String text, String instructions, Difficulty difficulty,
                    String topic, String imagePath, String courseId, List<QuestionAnswer> answers) {
        this.questionId = questionId;
        this.text = text;
        this.instructions = instructions;
        this.difficulty = difficulty;
        this.topic = topic;
        setImagePath(imagePath);
        this.courseId = courseId;
        this.rootQuestionId = questionId;
        this.versionNumber = 1;
        this.latest = true;
        this.answers = answers != null ? answers : new ArrayList<>();
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = QuestionIllustration.sanitizeFilename(imagePath);
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = QuestionIllustration.copy(imageData);
    }

    public boolean hasIllustration() {
        return QuestionIllustration.hasData(imageData);
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getRootQuestionId() {
        return rootQuestionId != null && !rootQuestionId.isBlank() ? rootQuestionId : questionId;
    }

    public void setRootQuestionId(String rootQuestionId) {
        this.rootQuestionId = rootQuestionId;
    }

    public int getVersionNumber() {
        return versionNumber <= 0 ? 1 : versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
    }

    public String versionStatusLabel() {
        return latest ? "Current" : "Historical";
    }

    public List<QuestionAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuestionAnswer> answers) {
        this.answers = answers;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public QuestionAnswer getCorrectAnswer() {
        return answers.stream().filter(QuestionAnswer::isCorrect).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return "[" + questionId + "] v" + getVersionNumber() + " " + versionStatusLabel()
                + " (" + topic + " / " + difficulty + ") " + text;
    }
}

package com.hsts.shared.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds Principal comparison rows from already-loaded exams and answers.
 * Adding a similar grouping later is one enum value plus a filter branch.
 */
public final class PrincipalReportAssembler {

    private PrincipalReportAssembler() {
    }

    public static PrincipalComparisonReport assemble(PrincipalReportType type, String filterValue,
                                                     List<Exam> exams, List<ExamAnswer> answers) {
        String filter = filterValue != null ? filterValue.trim() : "";
        List<Exam> examList = exams != null ? exams : List.of();
        List<ExamAnswer> answerList = answers != null ? answers : List.of();

        Map<String, List<Double>> scoresByExam = new HashMap<>();
        Map<String, ExamAnswer> studentAnswerByExam = new HashMap<>();
        for (ExamAnswer answer : answerList) {
            Double official = ExamStatisticsCalculator.officialConfirmedScore(answer);
            if (official == null || answer.getExamId() == null) {
                continue;
            }
            scoresByExam.computeIfAbsent(answer.getExamId(), key -> new ArrayList<>()).add(official);
            if (type == PrincipalReportType.STUDENT && filter.equals(answer.getStudentId())) {
                studentAnswerByExam.put(answer.getExamId(), answer);
            }
        }

        List<Exam> selected = new ArrayList<>();
        for (Exam exam : examList) {
            if (exam != null && matches(type, filter, exam, studentAnswerByExam)) {
                selected.add(exam);
            }
        }
        selected.sort(rowOrder(type, studentAnswerByExam));

        List<PrincipalComparisonRow> rows = new ArrayList<>();
        for (Exam exam : selected) {
            rows.add(toRow(type, exam, scoresByExam.getOrDefault(exam.getExamId(), List.of()),
                    studentAnswerByExam.get(exam.getExamId())));
        }
        return new PrincipalComparisonReport(type, filter, rows);
    }

    private static boolean matches(PrincipalReportType type, String filter, Exam exam,
                                   Map<String, ExamAnswer> studentAnswerByExam) {
        if (type == null || filter.isBlank()) {
            return false;
        }
        return switch (type) {
            case TEACHER -> filter.equals(exam.getCreatedByTeacherId());
            case COURSE -> filter.equals(exam.getCourseId());
            case STUDENT -> studentAnswerByExam.containsKey(exam.getExamId());
        };
    }

    private static Comparator<Exam> rowOrder(PrincipalReportType type,
                                             Map<String, ExamAnswer> studentAnswerByExam) {
        if (type == PrincipalReportType.STUDENT) {
            return Comparator
                    .comparing((Exam exam) -> submittedAt(studentAnswerByExam.get(exam.getExamId())),
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(exam -> nullToEmpty(exam.getExamId()));
        }
        return Comparator.comparing(exam -> nullToEmpty(exam.getExamId()));
    }

    private static LocalDateTime submittedAt(ExamAnswer answer) {
        return answer != null ? answer.getSubmittedAt() : null;
    }

    private static PrincipalComparisonRow toRow(PrincipalReportType type, Exam exam,
                                                List<Double> examScores, ExamAnswer studentAnswer) {
        ExamStatisticsCalculator.Snapshot snapshot = ExamStatisticsCalculator.fromScores(examScores);
        PrincipalComparisonRow row = new PrincipalComparisonRow();
        row.setExamId(exam.getExamId());
        row.setExamTitle(exam.getTitle() + " (v" + exam.getVersionNumber() + ")");
        row.setCourseId(exam.getCourseId());
        row.setTeacherId(exam.getCreatedByTeacherId());
        row.setConfirmedCount(snapshot.count);
        row.setMean(snapshot.mean);
        row.setMedian(snapshot.median);
        row.setDeciles(snapshot.deciles);
        if (type == PrincipalReportType.STUDENT && studentAnswer != null) {
            row.setStudentGrade(ExamStatisticsCalculator.officialConfirmedScore(studentAnswer));
        }
        return row;
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}

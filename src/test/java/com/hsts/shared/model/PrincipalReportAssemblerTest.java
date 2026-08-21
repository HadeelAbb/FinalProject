package com.hsts.shared.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Principal comparison assembly using confirmed official grades only.
 * No MySQL, OCSF, or JavaFX.
 */
public class PrincipalReportAssemblerTest {

    private static int failCount = 0;

    public static void main(String[] args) {
        Exam e1 = exam("E1", "CS101", "teacher1");
        Exam e2 = exam("E2", "CS101", "teacher2");
        Exam e3 = exam("E3", "MATH201", "teacher1");
        Exam e4 = exam("E4", "CS101", "teacher1");
        Exam e6 = exam("E6", "CS101", "teacher1");
        List<Exam> exams = List.of(e1, e2, e3, e4, e6);

        ExamAnswer e1s1 = confirmed("EA1", "E1", "student1", 60);
        ExamAnswer e1s2 = confirmed("EA2", "E1", "student2", 80);
        ExamAnswer e2s1 = confirmed("EA3", "E2", "student1", 70);
        ExamAnswer e2s2 = confirmed("EA4", "E2", "student2", 90);
        ExamAnswer e3pending = pending("EA5", "E3", "student1", 90);
        ExamAnswer e4override = confirmedOverride("EA6", "E4", "student1", 70, 85);
        ExamAnswer e6s1 = confirmed("EA8", "E6", "student1", 70);
        ExamAnswer e6s2 = confirmed("EA9", "E6", "student2", 90);

        List<ExamAnswer> answers = List.of(e1s1, e1s2, e2s1, e2s2, e3pending, e4override, e6s1, e6s2);

        PrincipalComparisonReport teacherReport = PrincipalReportAssembler.assemble(
                PrincipalReportType.TEACHER, "teacher1", exams, answers);
        PrincipalComparisonRow teacherE1 = row(teacherReport, "E1");
        PrincipalComparisonRow teacherE3 = row(teacherReport, "E3");
        PrincipalComparisonRow teacherE4 = row(teacherReport, "E4");
        PrincipalComparisonRow teacherE6 = row(teacherReport, "E6");
        check(teacherReport.getRows().size() == 4, "teacher comparison returns teacher1 exams only");
        check(row(teacherReport, "E2") == null, "teacher1 report does not include teacher2 exam");
        check(teacherE1 != null && teacherE1.getMean() == 70.0 && teacherE1.getMedian() == 70.0,
                "E1 mean = 70 and median = 70");
        check(teacherE6 != null && teacherE6.getMean() == 80.0 && teacherE6.getMedian() == 80.0,
                "E6 mean = 80 and median = 80");
        check(teacherE4 != null && teacherE4.getMean() == 85.0 && teacherE4.getMedian() == 85.0,
                "confirmed final grade 85 is used, not auto 70");
        check(teacherE3 != null && teacherE3.getConfirmedCount() == 0
                        && teacherE3.getMean() == null && teacherE3.getMedian() == null,
                "exam with only pending grades has no fake mean/median 0");

        PrincipalComparisonReport courseReport = PrincipalReportAssembler.assemble(
                PrincipalReportType.COURSE, "CS101", exams, answers);
        check(row(courseReport, "E1") != null && row(courseReport, "E2") != null
                        && row(courseReport, "E4") != null,
                "CS101 comparison includes exams from teacher1 and teacher2");
        check(row(courseReport, "E3") == null, "MATH201 exam is excluded from CS101 comparison");
        PrincipalComparisonRow courseE2 = row(courseReport, "E2");
        check(courseE2 != null && courseE2.getMean() == 80.0 && courseE2.getMedian() == 80.0,
                "E2 mean = 80 and median = 80");

        PrincipalComparisonReport studentReport = PrincipalReportAssembler.assemble(
                PrincipalReportType.STUDENT, "student1", exams, answers);
        check(studentReport.getRows().size() == 4, "student1 confirmed exams are E1, E2, E4, E6");
        check(row(studentReport, "E3") == null, "pending E3 is excluded from student comparison");
        check(row(studentReport, "E1") != null && row(studentReport, "E1").getStudentGrade() == 60.0,
                "student1 E1 grade is 60, not student2's 80");
        check(row(studentReport, "E2") != null && row(studentReport, "E2").getStudentGrade() == 70.0,
                "student1 E2 confirmed grade is 70");
        check(row(studentReport, "E4") != null && row(studentReport, "E4").getStudentGrade() == 85.0,
                "student comparison uses confirmed final 85");
        check(row(studentReport, "E1") != null && row(studentReport, "E1").getMean() == 70.0,
                "student row still includes exam-wide confirmed mean");

        ExamStatisticsCalculator.Snapshot odd = ExamStatisticsCalculator.fromScores(List.of(60.0, 70.0, 90.0));
        check(odd.median == 70.0, "median of 60,70,90 is 70");
        ExamStatisticsCalculator.Snapshot even = ExamStatisticsCalculator.fromScores(List.of(60.0, 70.0, 80.0, 90.0));
        check(even.median == 75.0, "median of 60,70,80,90 is 75");

        List<Double> boundaries = List.of(0.0, 9.0, 10.0, 19.0, 20.0, 89.0, 90.0, 99.0, 100.0);
        int[] expectedIndex = {0, 0, 1, 1, 2, 8, 9, 9, 9};
        for (int i = 0; i < boundaries.size(); i++) {
            check(GradeHistogramCalculator.bucketIndex(boundaries.get(i)) == expectedIndex[i],
                    boundaries.get(i) + " lands in exactly one bucket");
        }
        int[] boundaryCounts = ExamStatisticsCalculator.fromScores(boundaries).deciles;
        check(sum(boundaryCounts) == boundaries.size(),
                "each boundary grade appears in exactly one decile");
        check(boundaryCounts[9] == 3, "90, 99, and 100 are in the 90-100 bucket");

        ExamStatisticsCalculator.Snapshot empty = ExamStatisticsCalculator.fromScores(List.of());
        check(empty.count == 0 && empty.mean == null && empty.median == null,
                "no confirmed results: no crash and no fake mean 0");

        ExamAnswer weighted = confirmed("EA7", "E5", "student1", 40);
        Exam e5 = exam("E5", "CS101", "teacher1");
        PrincipalComparisonRow weightedRow = row(
                PrincipalReportAssembler.assemble(PrincipalReportType.TEACHER, "teacher1",
                        List.of(e5), List.of(weighted)),
                "E5");
        check(weightedRow != null && weightedRow.getMean() == 40.0,
                "weighted confirmed grade 40 is used as-is, not recalculated");

        System.out.println();
        if (failCount == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failCount + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static Exam exam(String examId, String courseId, String teacherId) {
        return new Exam(examId, courseId, "Exam " + examId, "", List.of(), 60, teacherId);
    }

    private static ExamAnswer confirmed(String id, String examId, String studentId, double score) {
        return confirmedOverride(id, examId, studentId, score, score);
    }

    private static ExamAnswer confirmedOverride(String id, String examId, String studentId,
                                                double auto, double fin) {
        ExamAnswer answer = new ExamAnswer(id, examId, studentId);
        answer.setAutoScore(auto);
        answer.setFinalScore(fin);
        answer.setGradeConfirmed(true);
        answer.setSubmittedAt(LocalDateTime.of(2026, 8, 20, 10, 0));
        return answer;
    }

    private static ExamAnswer pending(String id, String examId, String studentId, double auto) {
        ExamAnswer answer = new ExamAnswer(id, examId, studentId);
        answer.setAutoScore(auto);
        answer.setFinalScore(null);
        answer.setGradeConfirmed(false);
        answer.setSubmittedAt(LocalDateTime.of(2026, 8, 20, 11, 0));
        return answer;
    }

    private static PrincipalComparisonRow row(PrincipalComparisonReport report, String examId) {
        for (PrincipalComparisonRow candidate : report.getRows()) {
            if (examId.equals(candidate.getExamId())) {
                return candidate;
            }
        }
        return null;
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  OK  - " + description);
        } else {
            System.out.println("FAIL  - " + description);
            failCount++;
        }
    }
}

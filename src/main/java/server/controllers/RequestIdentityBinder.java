package server.controllers;

import com.hsts.shared.net.Request;
import com.hsts.shared.net.dto.AskBotQuestionData;
import com.hsts.shared.net.dto.ConfirmGradeData;
import com.hsts.shared.net.dto.CreateExamAutoData;
import com.hsts.shared.net.dto.CreateExamExecutionData;
import com.hsts.shared.net.dto.CreateExamManualData;
import com.hsts.shared.net.dto.CreateExamVersionData;
import com.hsts.shared.net.dto.CreateQuestionData;
import com.hsts.shared.net.dto.DeleteQuestionData;
import com.hsts.shared.net.dto.EditQuestionData;
import com.hsts.shared.net.dto.ExamApprovalDecisionData;
import com.hsts.shared.net.dto.ExtendExamTimeData;
import com.hsts.shared.net.dto.GetAvailableExamsData;
import com.hsts.shared.net.dto.GetBotHistoryData;
import com.hsts.shared.net.dto.GetExamAnswerCopyData;
import com.hsts.shared.net.dto.GetExamResultsData;
import com.hsts.shared.net.dto.GetMyExamsData;
import com.hsts.shared.net.dto.GetMyResultsData;
import com.hsts.shared.net.dto.GetPendingGradingData;
import com.hsts.shared.net.dto.StartExamData;
import com.hsts.shared.net.dto.SubmitExamData;
import com.hsts.shared.net.dto.SubmitExamForApprovalData;

/**
 * Pattern A: actor identity on a DTO is data, not proof.
 * After role authorization, overwrite actor ids with the authenticated user.
 * Resource ids (examId, questionId, courseId, examAnswerId) are left alone.
 */
public final class RequestIdentityBinder {

    private RequestIdentityBinder() {
    }

    public static void bindActor(Request request, AuthenticatedSession session) {
        if (request == null || session == null || session.getUserId() == null) {
            return;
        }
        Object payload = request.getPayload();
        if (payload == null) {
            return;
        }
        String userId = session.getUserId();

        if (payload instanceof StartExamData data) {
            data.setStudentId(userId);
        } else if (payload instanceof SubmitExamData data) {
            data.setStudentId(userId);
        } else if (payload instanceof GetAvailableExamsData data) {
            data.setStudentId(userId);
        } else if (payload instanceof GetMyResultsData data) {
            data.setStudentId(userId);
        } else if (payload instanceof GetExamAnswerCopyData data) {
            data.setStudentId(userId);
        } else if (payload instanceof AskBotQuestionData data) {
            data.setStudentId(userId);
        } else if (payload instanceof GetBotHistoryData data) {
            data.setStudentId(userId);
        } else if (payload instanceof CreateQuestionData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof EditQuestionData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof DeleteQuestionData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof CreateExamManualData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof CreateExamAutoData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof CreateExamVersionData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof SubmitExamForApprovalData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof GetMyExamsData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof GetPendingGradingData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof GetExamResultsData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof ConfirmGradeData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof ExtendExamTimeData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof CreateExamExecutionData data) {
            data.setTeacherId(userId);
        } else if (payload instanceof ExamApprovalDecisionData data) {
            data.setCoordinatorId(userId);
        }
    }
}

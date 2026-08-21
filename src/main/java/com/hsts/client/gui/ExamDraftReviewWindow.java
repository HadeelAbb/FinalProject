package com.hsts.client.gui;

import com.hsts.shared.model.Exam;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

/** Read-only view of a created exam draft. Does not save or edit. */
public class ExamDraftReviewWindow {

    @FXML private Label titleLabel;
    @FXML private Label metaLabel;
    @FXML private VBox questionsBox;
    @FXML private Label totalLabel;
    @FXML private Button closeButton;

    public static void open(Window owner, Exam exam) {
        if (exam == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    ExamDraftReviewWindow.class.getResource("/com/hsts/client/gui/exam_draft_review.fxml"));
            Parent root = loader.load();
            ExamDraftReviewWindow controller = loader.getController();
            controller.display(exam);

            Stage stage = new Stage();
            if (owner != null) {
                stage.initOwner(owner);
                stage.initModality(Modality.WINDOW_MODAL);
            }
            stage.setTitle("HSTS - Review Draft");
            stage.setScene(new Scene(root, 540, 640));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void display(Exam exam) {
        titleLabel.setText(exam.getTitle() != null ? exam.getTitle() : "Exam draft");
        metaLabel.setText(ExamDraftReviewFormatter.formatMeta(exam));
        questionsBox.getChildren().clear();
        for (String block : ExamDraftReviewFormatter.formatQuestions(exam)) {
            Label questionLabel = new Label(block);
            questionLabel.setWrapText(true);
            questionLabel.setMaxWidth(Double.MAX_VALUE);
            questionsBox.getChildren().add(questionLabel);
        }
        totalLabel.setText(ExamDraftReviewFormatter.formatTotal(exam));
    }

    @FXML
    void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}

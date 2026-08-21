package com.hsts.client.gui;

import com.hsts.shared.model.Question;
import com.hsts.shared.model.QuestionIllustration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;

/**
 * Client-only JavaFX preview. Shared Question/DTO classes stay byte[] only.
 */
public final class QuestionIllustrationView {

    private QuestionIllustrationView() {
    }

    public static void apply(ImageView view, Question question) {
        apply(view, question != null ? question.getImageData() : null);
    }

    public static void apply(ImageView view, byte[] imageData) {
        if (view == null) {
            return;
        }
        if (!QuestionIllustration.hasData(imageData)) {
            view.setImage(null);
            view.setVisible(false);
            view.setManaged(false);
            return;
        }
        try {
            Image image = new Image(new ByteArrayInputStream(imageData));
            if (image.isError()) {
                view.setImage(null);
                view.setVisible(false);
                view.setManaged(false);
                return;
            }
            view.setImage(image);
            view.setVisible(true);
            view.setManaged(true);
        } catch (Exception e) {
            view.setImage(null);
            view.setVisible(false);
            view.setManaged(false);
        }
    }

    public static ImageView preview(Question question, double fitWidth, double fitHeight) {
        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setFitWidth(fitWidth);
        view.setFitHeight(fitHeight);
        apply(view, question);
        return view;
    }
}

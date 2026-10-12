package lodgebilling.view;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * The login screen's backdrop: the same slat picture as the rest of the app, sweeping
 * slowly sideways and back. The picture is drawn wider than the window so its edges
 * never show.
 */
public class SlatBackground extends Pane {

    /** How far the picture overhangs each side of the window, and so how far it can sweep. */
    private static final double SWEEP = 65;
    private static final Duration HALF_CYCLE = Duration.seconds(5);

    private final Image lightPicture = new Image(getClass().getResource("/bg-slats.jpg").toExternalForm());
    private final Image darkPicture = new Image(getClass().getResource("/bg-slats-white.jpg").toExternalForm());
    private final ImageView picture = new ImageView();
    private final TranslateTransition sweep = new TranslateTransition(HALF_CYCLE, picture);

    public SlatBackground(boolean dark) {
        setMouseTransparent(true);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        picture.setPreserveRatio(false);
        getChildren().add(picture);
        setDark(dark);

        sweep.setFromX(-SWEEP);
        sweep.setToX(SWEEP);
        sweep.setInterpolator(Interpolator.EASE_BOTH);
        sweep.setAutoReverse(true);
        sweep.setCycleCount(Animation.INDEFINITE);

        widthProperty().addListener((obs, old, w) -> fit());
        heightProperty().addListener((obs, old, h) -> fit());
        sweep.play();
    }

    public final void setDark(boolean dark) {
        picture.setImage(dark ? darkPicture : lightPicture);
        fit();
    }

    public void stop() {
        sweep.stop();
    }

    /** Scales the picture to cover the window plus the overhang, keeping its proportions. */
    private void fit() {
        Image image = picture.getImage();
        double width = getWidth() + 2 * SWEEP;
        double scale = Math.max(width / image.getWidth(), getHeight() / image.getHeight());
        double w = image.getWidth() * scale;
        double h = image.getHeight() * scale;
        picture.setFitWidth(w);
        picture.setFitHeight(h);
        picture.setLayoutX(-SWEEP - (w - width) / 2);
        picture.setLayoutY((getHeight() - h) / 2);
    }
}

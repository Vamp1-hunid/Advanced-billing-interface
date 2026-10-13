package lodgebilling.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.util.Duration;

/** Small animations shared by the screens: parts sliding in, and buttons lifting under the mouse. */
public final class Motion {

    private static final Duration ENTER_TIME = Duration.millis(550);
    private static final Duration STAGGER = Duration.millis(90);
    private static final double RISE = 26;
    private static final Duration HOVER_TIME = Duration.millis(140);
    private static final double HOVER_SCALE = 1.04;

    private Motion() { }

    /** Fades the parts in while they rise into place, one shortly after another. */
    public static void enter(Node... parts) {
        for (int i = 0; i < parts.length; i++) {
            Node part = parts[i];
            part.setOpacity(0);
            part.setTranslateY(RISE);

            FadeTransition fade = new FadeTransition(ENTER_TIME, part);
            fade.setToValue(1);
            TranslateTransition rise = new TranslateTransition(ENTER_TIME, part);
            rise.setToY(0);

            ParallelTransition entrance = new ParallelTransition(fade, rise);
            entrance.setInterpolator(Interpolator.EASE_OUT);
            entrance.setDelay(STAGGER.multiply(i));
            entrance.play();
        }
    }

    /** Makes every button inside the given screen grow slightly while the mouse is over it. */
    public static void liftButtons(Parent screen) {
        for (Node button : screen.lookupAll(".button")) {
            ScaleTransition grow = new ScaleTransition(HOVER_TIME, button);
            button.hoverProperty().addListener((obs, was, hovering) -> {
                double scale = hovering ? HOVER_SCALE : 1;
                grow.stop();
                grow.setToX(scale);
                grow.setToY(scale);
                grow.playFromStart();
            });
        }
    }
}

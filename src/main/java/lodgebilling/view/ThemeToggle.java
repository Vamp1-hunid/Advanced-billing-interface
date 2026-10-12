package lodgebilling.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lodgebilling.model.Theme;

/**
 * A glass switch for light/dark mode: a frosted pill with a thumb that slides
 * across while the day sky (sun, clouds) cross-fades into the night sky (moon,
 * stars). It follows the theme property, so changing the theme from anywhere
 * (menu, other toggle) moves it too.
 */
public class ThemeToggle extends StackPane {

    private static final double WIDTH = 78;
    private static final double HEIGHT = 40;
    private static final double THUMB = 32;
    private static final double GAP = (HEIGHT - THUMB) / 2;
    private static final double TRAVEL = WIDTH - THUMB - 2 * GAP;
    private static final Duration SPEED = Duration.millis(380);
    // Slight overshoot at the end so the thumb settles with a little bounce
    private static final Interpolator SPRING = new Interpolator() {
        @Override
        protected double curve(double t) {
            double c = 1.4; // how far it overshoots
            double u = t - 1;
            return 1 + (c + 1) * u * u * u + c * u * u;
        }
    };

    private final StackPane dayLayer = layer("toggle-day", "☁  ☁", Pos.CENTER_RIGHT);
    private final StackPane nightLayer = layer("toggle-night", "✦ ✧", Pos.CENTER_LEFT);
    private final StackPane thumb = new StackPane();
    private final Label sun = icon("☀", "toggle-sun");
    private final Label moon = icon("☾", "toggle-moon");

    // Kept in a field: the model only holds it weakly, so it lives as long as the toggle
    private final ChangeListener<Theme> themeListener;

    public ThemeToggle(ObjectProperty<Theme> theme, Runnable onToggle) {
        getStyleClass().add("theme-toggle");
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setFocusTraversable(true);
        Tooltip.install(this, new Tooltip("Switch between light and dark mode"));

        thumb.getStyleClass().add("toggle-thumb");
        thumb.setMinSize(THUMB, THUMB);
        thumb.setPrefSize(THUMB, THUMB);
        thumb.setMaxSize(THUMB, THUMB);
        thumb.getChildren().addAll(sun, moon);
        StackPane.setAlignment(thumb, Pos.CENTER_LEFT);
        StackPane.setMargin(thumb, new Insets(0, 0, 0, GAP));

        getChildren().addAll(dayLayer, nightLayer, thumb);

        setOnMouseClicked(e -> {
            requestFocus();
            onToggle.run();
        });
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                onToggle.run();
                e.consume();
            }
        });

        showImmediately(theme.get());
        themeListener = (obs, old, now) -> slideTo(now);
        theme.addListener(new WeakChangeListener<>(themeListener));
    }

    // ---------- Drawing ----------

    private static StackPane layer(String styleClass, String decoration, Pos decorationSide) {
        Label deco = new Label(decoration);
        deco.getStyleClass().add("toggle-decoration");
        StackPane layer = new StackPane(deco);
        layer.getStyleClass().addAll("toggle-track", styleClass);
        StackPane.setAlignment(deco, decorationSide);
        StackPane.setMargin(deco, new Insets(0, 14, 0, 14));
        layer.setMouseTransparent(true);
        return layer;
    }

    private static Label icon(String symbol, String styleClass) {
        Label label = new Label(symbol);
        label.getStyleClass().addAll("toggle-icon", styleClass);
        return label;
    }

    // ---------- States ----------

    /** Sets the look for a theme with no animation (used when the toggle first appears). */
    private void showImmediately(Theme theme) {
        boolean dark = theme == Theme.DARK;
        thumb.setTranslateX(dark ? TRAVEL : 0);
        nightLayer.setOpacity(dark ? 1 : 0);
        sun.setOpacity(dark ? 0 : 1);
        moon.setOpacity(dark ? 1 : 0);
        sun.setRotate(dark ? 90 : 0);
        moon.setRotate(dark ? 0 : -90);
        announce(dark);
    }

    /** Slides the thumb across while the sky and the sun/moon icons cross-fade. */
    private void slideTo(Theme theme) {
        boolean dark = theme == Theme.DARK;

        TranslateTransition slide = new TranslateTransition(SPEED, thumb);
        slide.setToX(dark ? TRAVEL : 0);
        slide.setInterpolator(SPRING);

        FadeTransition sky = new FadeTransition(SPEED, nightLayer);
        sky.setToValue(dark ? 1 : 0);

        ParallelTransition all = new ParallelTransition(slide, sky,
                swap(sun, dark ? 0 : 1, dark ? 90 : 0),
                swap(moon, dark ? 1 : 0, dark ? 0 : -90));
        all.play();
        announce(dark);
    }

    private static ParallelTransition swap(Label icon, double opacity, double angle) {
        FadeTransition fade = new FadeTransition(SPEED, icon);
        fade.setToValue(opacity);
        RotateTransition spin = new RotateTransition(SPEED, icon);
        spin.setToAngle(angle);
        spin.setInterpolator(Interpolator.EASE_BOTH);
        return new ParallelTransition(fade, spin);
    }

    private void announce(boolean dark) {
        setAccessibleText(dark ? "Dark mode on. Switch to light mode." : "Light mode on. Switch to dark mode.");
    }
}

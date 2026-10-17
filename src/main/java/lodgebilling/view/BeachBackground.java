package lodgebilling.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PathTransition;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import lodgebilling.model.Theme;

/**
 * The window's backdrop: a moving beach. In light mode the sun climbs out of the
 * sea and gulls wheel over sparkling water (sunrise); in dark mode the sun sinks
 * below the horizon, the sky turns to embers and the first stars come out
 * (sunset). Waves roll in as overlapping crests with foam lines, clouds drift
 * across the sky, palm silhouettes sway on the shore and — after dusk — a rare
 * falling star slips down the evening sky. Switching themes crossfades the whole
 * scene from one mood to the other. Everything is placed as a fraction of the
 * window, so it fits any size.
 */
public class BeachBackground extends Pane {

    /** Where the sea meets the sky, as a fraction of the window height. */
    private static final double HORIZON = 0.58;
    // The same seed every time, so the layout does not jump about between runs
    private static final long SEED = 261;

    private static final int WAVE_COUNT = 5;
    private static final int SPARKLE_COUNT = 70;
    private static final int STAR_COUNT = 90;
    private static final int CLOUD_COUNT = 5;
    private static final int BIRD_COUNT = 3;
    private static final int WAVE_POINTS = 40;

    // ---------- Sunrise (light theme) ----------
    private static final Color[] SUNRISE_SKY = {
        Color.web("#2c4e86"), Color.web("#7fa8d9"), Color.web("#ffdca8"),
        Color.web("#ffb27d"), Color.web("#ff9a68"), Color.web("#ffc489") };
    private static final Color[] SUNRISE_SEA = {
        Color.web("#2f8aa8"), Color.web("#2b7f9e"), Color.web("#155e78") };
    private static final Color SUNRISE_CORE = Color.web("#fffbe8");
    private static final Color SUNRISE_HALO = Color.web("#ffd98a");
    private static final Color SUNRISE_GLINT = Color.web("#ffe9b8");
    private static final Color SUNRISE_NEAR_WAVE = Color.web("#57b9cf");
    private static final Color SUNRISE_FAR_WAVE = Color.web("#2f8aa8");
    private static final Color SUNRISE_SAND = Color.web("#eccf9c");
    private static final Color SUNRISE_PALM_NEAR = Color.web("#1c2b22");
    private static final Color SUNRISE_PALM_FAR = Color.web("#22362a");
    private static final double SUNRISE_SUN_Y = 0.30;   // well above the horizon

    // ---------- Sunset (dark theme) ----------
    private static final Color[] SUNSET_SKY = {
        Color.web("#140a2b"), Color.web("#3d1b56"), Color.web("#7c2f5c"),
        Color.web("#c04a55"), Color.web("#ef7a4a"), Color.web("#ffb45f") };
    private static final Color[] SUNSET_SEA = {
        Color.web("#3c2350"), Color.web("#241638"), Color.web("#100a20") };
    private static final Color SUNSET_CORE = Color.web("#fff3c2");
    private static final Color SUNSET_HALO = Color.web("#ff7a35");
    private static final Color SUNSET_GLINT = Color.web("#ff9d5c");
    private static final Color SUNSET_NEAR_WAVE = Color.web("#6a3f74");
    private static final Color SUNSET_FAR_WAVE = Color.web("#3c2350");
    private static final Color SUNSET_SAND = Color.web("#3a2c46");
    private static final Color SUNSET_PALM_NEAR = Color.web("#160f22");
    private static final Color SUNSET_PALM_FAR = Color.web("#1d1430");
    private static final double SUNSET_SUN_Y = 0.615;   // half sunk below the horizon

    // Gradient stops for a six-band sky and a three-band sea
    private static final double[] SKY_POS = { 0.00, 0.30, 0.52, 0.72, 0.86, 1.00 };
    private static final double[] SEA_POS = { 0.00, 0.45, 1.00 };

    /** A palm silhouette drawn in fixed units; scaled and moved into place later. */
    private static final String PALM_PATH =
            "M46,138 C45,112 49,84 55,56"
          + " C50,52 44,50 38,50 C44,46 50,45 56,47"
          + " C55,42 56,38 58,34 C60,38 61,43 61,48"
          + " C66,45 72,44 78,45 C72,48 66,50 62,52"
          + " C66,56 68,60 69,65 C65,60 61,56 58,54"
          + " C56,58 55,62 55,66 C53,61 52,56 53,52"
          + " C48,54 43,58 39,63 C43,57 48,53 52,51 Z"
          + " M56,52 C42,44 28,42 14,46 C28,46 42,50 54,58 Z"
          + " M58,50 C50,34 40,24 26,18 C40,26 50,38 56,54 Z"
          + " M60,50 C62,32 68,20 80,10 C70,22 64,36 62,54 Z"
          + " M62,52 C76,40 92,36 106,38 C92,42 78,48 64,58 Z"
          + " M62,56 C76,52 90,54 100,62 C88,56 76,56 63,62 Z"
          + " M48,138 L58,138 L56,120 L50,120 Z";
    private static final double PALM_WIDTH_UNITS = 100;
    private static final double PALM_HEIGHT_UNITS = 140;
    private static final double PALM_BASE_Y_UNITS = 138;

    /** One cloud, puffy along the top and flat underneath. */
    private static final String CLOUD_PATH =
            "M10,30 Q14,14 30,18 Q36,4 54,10 Q70,2 76,16 Q94,14 96,30 Q80,38 52,36 Q24,40 10,30 Z";

    // How each wave band behaves: travel speed, amplitude, depth in the sea, wavelength
    private static final double[] WAVE_SPEC = {
        0.05, 0.012, 0.10, 1.6,
        0.07, 0.016, 0.26, 2.1,
        0.09, 0.020, 0.44, 2.7,
        0.12, 0.026, 0.62, 3.4,
        0.15, 0.032, 0.80, 4.2,
    };

    /** How far the scene has travelled between sunrise (0) and sunset (1). */
    private double phase;
    private Double targetPhase;
    private AnimationTimer morph;
    private double seconds;

    private final Rectangle sky = new Rectangle();
    private final Rectangle sea = new Rectangle();
    private final Circle sunHalo = new Circle();
    private final Circle sunCore = new Circle();
    private final Rectangle glint = new Rectangle();
    private final Polygon sand = new Polygon();
    private final Line horizon = new Line();
    private final Group starField = new Group();
    private final List<Circle> stars = new ArrayList<>();
    private final double[][] starSpots;
    private final Group waveGroup = new Group();
    private final List<Polygon> waveBodies = new ArrayList<>();
    private final List<Polygon> foamLines = new ArrayList<>();
    private final Group sparkleWrap = new Group();
    private final List<Circle> sparkles = new ArrayList<>();
    private final double[][] sparkleSpots;
    private final Group cloudGroup = new Group();
    private final List<Path> cloudLanes = new ArrayList<>();
    private final List<PathTransition> cloudDrifts = new ArrayList<>();
    private final double[] cloudSpots;
    private final Group birdGroup = new Group();
    private final List<Path> birdLanes = new ArrayList<>();
    private final List<PathTransition> birdFlights = new ArrayList<>();
    private final SVGPath palmNear = new SVGPath();
    private final SVGPath palmFar = new SVGPath();
    private final Rotate nearSway = new Rotate(0, 52, PALM_BASE_Y_UNITS);
    private final Rotate farSway = new Rotate(0, 52, PALM_BASE_Y_UNITS);
    private final Line shootingStar = new Line();
    private final List<Animation> motions = new ArrayList<>();
    private final AnimationTimer waves;

    public BeachBackground() {
        setMouseTransparent(true);
        Random random = new Random(SEED);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        sky.widthProperty().bind(widthProperty());
        sky.heightProperty().bind(heightProperty().multiply(HORIZON));

        sea.widthProperty().bind(widthProperty());
        sea.heightProperty().bind(heightProperty().multiply(1 - HORIZON));
        sea.layoutYProperty().bind(heightProperty().multiply(HORIZON));

        // The sun with a soft halo, mirrored by a shimmering column on the water
        sunHalo.setEffect(new GaussianBlur(26));
        sunCore.setEffect(new GaussianBlur(3));
        glint.setEffect(new GaussianBlur(10));
        glint.setBlendMode(BlendMode.SCREEN);

        // Five bands of water; each body has a thin foam line riding its crest
        waveGroup.setEffect(new GaussianBlur(2.5));
        for (int i = 0; i < WAVE_COUNT; i++) {
            Polygon body = new Polygon();
            Polygon foam = new Polygon();
            foam.setFill(Color.TRANSPARENT);
            foam.setStrokeWidth(Math.max(1.5, 3.5 - i * 0.4));
            waveBodies.add(body);
            foamLines.add(foam);
            waveGroup.getChildren().addAll(body, foam);
        }

        // Sparkling points where the sun's reflection touches the water
        sparkleSpots = new double[SPARKLE_COUNT][2];
        for (int i = 0; i < SPARKLE_COUNT; i++) {
            sparkleSpots[i][0] = random.nextDouble();
            sparkleSpots[i][1] = Math.pow(random.nextDouble(), 1.6);
            Circle s = new Circle(1 + random.nextDouble() * 1.8, Color.web("#ffffff"));
            s.setOpacity(0.2 + random.nextDouble() * 0.6);
            sparkles.add(s);
            if (random.nextBoolean()) {
                FadeTransition twinkle = new FadeTransition(
                        Duration.seconds(0.9 + random.nextDouble() * 2.2), s);
                twinkle.setFromValue(s.getOpacity());
                twinkle.setToValue(0.05);
                twinkle.setAutoReverse(true);
                twinkle.setCycleCount(Animation.INDEFINITE);
                motions.add(twinkle);
            }
        }
        sparkleWrap.getChildren().setAll(sparkles);

        // Faint stars: invisible at sunrise, they fade in as the dusk deepens
        starSpots = new double[STAR_COUNT][2];
        for (int i = 0; i < STAR_COUNT; i++) {
            starSpots[i][0] = random.nextDouble();
            starSpots[i][1] = Math.pow(random.nextDouble(), 1.5) * 0.5;
            Circle star = new Circle(0.4 + random.nextDouble() * 1.1, Color.web("#fff6e0"));
            star.setOpacity(0.3 + random.nextDouble() * 0.6);
            stars.add(star);
            starField.getChildren().add(star);
            if (random.nextDouble() < 0.4) {
                FadeTransition twinkle = new FadeTransition(
                        Duration.seconds(1.4 + random.nextDouble() * 3), star);
                twinkle.setFromValue(star.getOpacity());
                twinkle.setToValue(0.08);
                twinkle.setDelay(Duration.seconds(random.nextDouble() * 3));
                twinkle.setAutoReverse(true);
                twinkle.setCycleCount(Animation.INDEFINITE);
                motions.add(twinkle);
            }
        }

        // Clouds drifting slowly along wide lanes high in the sky
        cloudSpots = new double[CLOUD_COUNT * 2];
        for (int i = 0; i < CLOUD_COUNT; i++) {
            cloudSpots[i * 2] = 0.05 + random.nextDouble() * 0.3;     // height in the sky
            cloudSpots[i * 2 + 1] = 0.7 + random.nextDouble() * 0.9;  // size
            SVGPath cloud = new SVGPath();
            cloud.setContent(CLOUD_PATH);
            cloud.setFill(Color.web("#ffffff", 0.55));
            cloud.setEffect(new GaussianBlur(4));
            cloudGroup.getChildren().add(cloud);
            Path lane = new Path();
            cloudLanes.add(lane);
            PathTransition drift = new PathTransition(
                    Duration.seconds(70 + random.nextDouble() * 60), lane, cloud);
            drift.setCycleCount(Animation.INDEFINITE);
            drift.setDelay(Duration.seconds(-random.nextDouble() * 60));
            cloudDrifts.add(drift);
        }

        // Gulls gliding over the water, flapping as they go
        for (int i = 0; i < BIRD_COUNT; i++) {
            SVGPath wings = new SVGPath();
            wings.setContent("M0,6 Q6,0 12,5 Q18,0 24,6");
            wings.setStroke(Color.web("#233039"));
            wings.setStrokeWidth(2);
            wings.setFill(Color.TRANSPARENT);
            Group bird = new Group(wings);
            birdGroup.getChildren().add(bird);
            Timeline flap = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(bird.scaleYProperty(), 1.0)),
                    new KeyFrame(Duration.millis(260), new KeyValue(bird.scaleYProperty(), 0.55)),
                    new KeyFrame(Duration.millis(520), new KeyValue(bird.scaleYProperty(), 1.0)));
            flap.setCycleCount(Animation.INDEFINITE);
            motions.add(flap);
            Path lane = new Path();
            birdLanes.add(lane);
            PathTransition fly = new PathTransition(
                    Duration.seconds(26 + random.nextDouble() * 18), lane, bird);
            fly.setCycleCount(Animation.INDEFINITE);
            fly.setDelay(Duration.seconds(-random.nextDouble() * 20));
            birdFlights.add(fly);
        }

        // Palms on the shore, rocking gently in the sea breeze
        palmNear.setContent(PALM_PATH);
        palmFar.setContent(PALM_PATH);
        palmNear.getTransforms().add(nearSway);
        palmFar.getTransforms().add(farSway);
        motions.add(sway(nearSway, 2.6, 1.6, 1.2));
        motions.add(sway(farSway, 3.4, -1.2, 1.0));

        horizon.setStrokeWidth(1);

        // On rare evenings a falling star slips down the dusk sky
        shootingStar.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(1, Color.web("#ffffff", 0.9))));
        shootingStar.setStrokeWidth(1.6);
        shootingStar.setOpacity(0);
        motions.add(shootingStarFlight(random));

        getChildren().addAll(sky, starField, sunHalo, sunCore, sea, glint,
                waveGroup, sparkleWrap, cloudGroup, horizon, sand, palmFar,
                birdGroup, palmNear, shootingStar);

        widthProperty().addListener((obs, old, w) -> placeStillParts());
        heightProperty().addListener((obs, old, h) -> placeStillParts());

        waves = new AnimationTimer() {
            @Override
            public void handle(long now) {
                moveWaves(now / 1_000_000_000.0);
            }
        };
        paintScene();
        motions.forEach(Animation::play);
        cloudDrifts.forEach(Animation::play);
        birdFlights.forEach(Animation::play);
        waves.start();
    }

    /** Starts the crossfade between sunrise and sunset whenever the theme changes. */
    public void bindTheme(ObjectProperty<Theme> theme) {
        boundTheme = theme;
        applyTheme(theme.get());
        theme.addListener(themeListener);
    }

    /** Lets go of the shared theme, for when this screen is replaced. */
    public void unbindTheme() {
        if (boundTheme != null) {
            boundTheme.removeListener(themeListener);
            boundTheme = null;
        }
    }

    private ObjectProperty<Theme> boundTheme;
    // Kept in a field so it can be removed again in unbindTheme (the model holds it weakly)
    private final ThemeChangeListener themeListener = new ThemeChangeListener();

    private class ThemeChangeListener implements javafx.beans.value.ChangeListener<Theme> {
        @Override
        public void changed(javafx.beans.value.ObservableValue<? extends Theme> obs,
                            Theme old, Theme now) {
            applyTheme(now);
        }
    }

    private void applyTheme(Theme theme) {
        targetPhase = theme == Theme.DARK ? 1 : 0;
        boolean dark = theme == Theme.DARK;
        for (Animation a : birdFlights) {
            if (dark) {
                a.pause();       // the gulls roost once the sun has gone
            } else {
                a.play();
            }
        }
        if (morph == null) {
            morph = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (targetPhase == null) {
                        stop();
                        morph = null;
                        return;
                    }
                    double d = targetPhase - phase;
                    if (Math.abs(d) < 0.004) {
                        phase = targetPhase;
                        targetPhase = null;
                    } else {
                        phase += d * 0.03;             // an unhurried, golden-hour fade
                    }
                    paintScene();
                    moveWaves(seconds);               // recolour the moving parts too
                }
            };
        }
        morph.start();
    }

    /** Repaints every coloured part of the scene for the current phase. */
    private void paintScene() {
        double p = phase;
        sky.setFill(verticalMix(SUNRISE_SKY, SUNSET_SKY, SKY_POS, p));
        sea.setFill(verticalMix(SUNRISE_SEA, SUNSET_SEA, SEA_POS, p));
        sunCore.setFill(mix(SUNRISE_CORE, SUNSET_CORE, p));
        sunHalo.setFill(mix(SUNRISE_HALO, SUNSET_HALO, p)
                .deriveColor(0, 1, 1, lerp(0.55, 0.8, p)));
        glint.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, mix(SUNRISE_GLINT, SUNSET_GLINT, p)
                        .deriveColor(0, 1, 1, 1, lerp(0.55, 0.7, p))),
                new Stop(1, Color.TRANSPARENT)));
        sand.setFill(mix(SUNRISE_SAND, SUNSET_SAND, p));
        horizon.setStroke(Color.web("#ffffff", lerp(0.55, 0.25, p)));
        starField.setOpacity(smooth(Math.max(0, (p - 0.35) / 0.65)));

        for (int i = 0; i < WAVE_COUNT; i++) {
            double depth = i / (double) (WAVE_COUNT - 1);      // 0 = far, 1 = near
            Color body = mix(mix(SUNRISE_FAR_WAVE, SUNRISE_NEAR_WAVE, depth),
                             mix(SUNSET_FAR_WAVE, SUNSET_NEAR_WAVE, depth), p);
            waveBodies.get(i).setFill(body.deriveColor(0, 1, 1, lerp(0.55, 0.9, depth)));
            foamLines.get(i).setStroke(mix(Color.web("#ffffff", 0.75),
                    Color.web("#ffd9a8", 0.5), p));
        }
        for (Circle s : sparkles) {
            s.setFill(mix(Color.web("#ffffff"), SUNSET_GLINT, p));
        }
        Paint cloudPaint = Color.web("#ffffff", lerp(0.55, 0.22, p));
        for (var node : cloudGroup.getChildren()) {
            ((SVGPath) node).setFill(cloudPaint);
        }
        palmNear.setFill(mix(SUNRISE_PALM_NEAR, SUNSET_PALM_NEAR, p));
        palmFar.setFill(mix(SUNRISE_PALM_FAR, SUNSET_PALM_FAR, p));
        birdGroup.setOpacity(lerp(1, 0.15, p));
    }

    /** Shapes the rolling crests for this moment: two offset swells per band. */
    private void moveWaves(double now) {
        seconds = now;
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        double seaTop = h * HORIZON;
        double seaHeight = h - seaTop;
        for (int i = 0; i < WAVE_COUNT; i++) {
            double speed = WAVE_SPEC[i * 4];
            double amp = WAVE_SPEC[i * 4 + 1];
            double base = WAVE_SPEC[i * 4 + 2];
            double wl = WAVE_SPEC[i * 4 + 3];
            buildCrest(waveBodies.get(i), w, seaTop, seaHeight, base, amp, wl,
                    now * speed, 0.0, true);
            buildCrest(foamLines.get(i), w, seaTop, seaHeight, base, amp * 0.6,
                    wl * 1.37, now * speed * 1.23 + 1.7, 0.012, false);
        }
        // the sun's reflection column sits under the sun on the water
        glint.setX(w * 0.5 - w * 0.055);
        glint.setY(seaTop);
        glint.setWidth(w * 0.11);
        glint.setHeight(seaHeight * 0.9);
        // sparkles wander with the swell
        for (int i = 0; i < sparkles.size(); i++) {
            Circle s = sparkles.get(i);
            double u = sparkleSpots[i][0];
            double v = sparkleSpots[i][1];
            double bob = Math.sin(now * (0.6 + v) + i * 1.3) * h * 0.004;
            s.setCenterX(u * w);
            s.setCenterY(seaTop + v * seaHeight * 0.8 + bob);
        }
    }

    /** One wave: a sine-and-a-half crest filled down to the bottom, or just its ridge. */
    private void buildCrest(Polygon poly, double w, double seaTop, double seaHeight,
                            double base, double amp, double wl, double t,
                            double yShift, boolean filled) {
        List<Double> pts = new ArrayList<>();
        if (filled) {
            pts.add(-4.0);
            pts.add(seaTop + seaHeight + 4);
        }
        for (int i = 0; i <= WAVE_POINTS; i++) {
            double u = i / (double) WAVE_POINTS;
            double y = seaTop + seaHeight * (base + yShift)
                    + seaHeight * amp * Math.sin(u * Math.PI * 2 * wl + t * 2 * Math.PI)
                    + seaHeight * amp * 0.4 * Math.sin(u * Math.PI * 5.1 - t * 1.6);
            pts.add(u * w);
            pts.add(y);
        }
        if (filled) {
            pts.add(w + 4.0);
            pts.add(seaTop + seaHeight + 4);
        }
        poly.getPoints().setAll(pts);
    }

    /** Places the sized parts: sun, stars, clouds, lanes, palms, sand, horizon. */
    private void placeStillParts() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        double r = Math.min(w, h);
        sunCore.setRadius(r * 0.055);
        sunHalo.setRadius(r * 0.16);
        sunCore.setCenterX(w * 0.5);
        sunHalo.setCenterX(w * 0.5);
        double sunY = h * lerp(SUNRISE_SUN_Y, SUNSET_SUN_Y, phase);
        sunCore.setCenterY(sunY);
        sunHalo.setCenterY(sunY);

        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).setCenterX(starSpots[i][0] * w);
            stars.get(i).setCenterY(starSpots[i][1] * h);
        }
        for (int i = 0; i < CLOUD_COUNT; i++) {
            Path lane = cloudLanes.get(i);
            lane.getElements().clear();
            double y = h * cloudSpots[i * 2];
            lane.getElements().add(new MoveTo(-140, y));
            lane.getElements().add(new CubicCurveTo(w * 0.3, y - h * 0.02,
                    w * 0.7, y + h * 0.02, w + 140, y));
            SVGPath cloud = (SVGPath) cloudGroup.getChildren().get(i);
            double scale = cloudSpots[i * 2 + 1] * (r / 600 + 0.5);
            cloud.setScaleX(scale);
            cloud.setScaleY(scale * 0.7);
        }
        for (int i = 0; i < BIRD_COUNT; i++) {
            Path lane = birdLanes.get(i);
            lane.getElements().clear();
            double y = h * (0.14 + i * 0.09);
            lane.getElements().add(new MoveTo(-60, y));
            lane.getElements().add(new CubicCurveTo(w * 0.3, y - h * 0.05,
                    w * 0.55, y + h * 0.04, w * 0.78, y));
            lane.getElements().add(new CubicCurveTo(w * 0.95, y - h * 0.03,
                    w + 40, y + h * 0.02, w + 80, y));
            Group bird = (Group) birdGroup.getChildren().get(i);
            double scale = (0.7 + i * 0.25) * (r / 700 + 0.5);
            bird.setScaleX(scale);
            bird.setScaleY(scale);
        }

        horizon.setStartX(0);
        horizon.setEndX(w);
        horizon.setStartY(h * HORIZON);
        horizon.setEndY(h * HORIZON);

        // a curving strip of sand along the bottom edge
        List<Double> pts = new ArrayList<>();
        pts.add(0.0);
        pts.add(h);
        int n = 24;
        for (int i = 0; i <= n; i++) {
            double u = i / (double) n;
            pts.add(u * w);
            pts.add(h - h * (0.05 + 0.03 * Math.sin(u * Math.PI * 1.3 + 0.4)));
        }
        pts.add(w);
        pts.add(h);
        sand.getPoints().setAll(pts);

        placePalm(palmNear, w * 0.86, h, r * 0.30, nearSway);
        placePalm(palmFar, w * 0.06, h * 0.985, r * 0.22, farSway);
    }

    /** Scales a palm to the window and pins its pivot at the foot of the trunk. */
    private static void placePalm(SVGPath palm, double x, double baseline,
                                  double size, Rotate sway) {
        double scale = size / PALM_HEIGHT_UNITS;
        palm.setTranslateX(x - PALM_WIDTH_UNITS * scale / 2);
        palm.setTranslateY(baseline - PALM_BASE_Y_UNITS * scale);
        palm.setScaleX(scale);
        palm.setScaleY(scale);
        sway.setPivotX(52);
        sway.setPivotY(PALM_BASE_Y_UNITS);
    }

    /** A slow rocking turn about the trunk's foot. */
    private static Timeline sway(Rotate rotate, double seconds, double degrees, double delay) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(rotate.angleProperty(), 0)),
                new KeyFrame(Duration.seconds(seconds), new KeyValue(rotate.angleProperty(), degrees)),
                new KeyFrame(Duration.seconds(seconds * 2), new KeyValue(rotate.angleProperty(), 0)));
        t.setCycleCount(Animation.INDEFINITE);
        t.setDelay(Duration.seconds(delay));
        return t;
    }

    /** Every so often a star streaks down across the upper sky. */
    private Animation shootingStarFlight(Random random) {
        Timeline flight = new Timeline();
        flight.setCycleCount(Animation.INDEFINITE);
        flight.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, e -> aimShootingStar(random),
                        new KeyValue(shootingStar.opacityProperty(), 0),
                        new KeyValue(shootingStar.translateXProperty(), 0),
                        new KeyValue(shootingStar.translateYProperty(), 0)),
                new KeyFrame(Duration.millis(150),
                        new KeyValue(shootingStar.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(shootingStar.opacityProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(shootingStar.translateXProperty(), 260, Interpolator.EASE_OUT),
                        new KeyValue(shootingStar.translateYProperty(), 110, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.seconds(11)));
        flight.setDelay(Duration.seconds(4));
        return flight;
    }

    private void aimShootingStar(Random random) {
        double x = getWidth() * (0.05 + random.nextDouble() * 0.6);
        double y = getHeight() * (0.04 + random.nextDouble() * 0.2);
        shootingStar.setStartX(x - 90);
        shootingStar.setStartY(y - 38);
        shootingStar.setEndX(x);
        shootingStar.setEndY(y);
    }

    /** Stops every animation, for when the screen is being replaced. */
    public void stop() {
        waves.stop();
        if (morph != null) {
            morph.stop();
        }
        cloudDrifts.forEach(Animation::stop);
        birdFlights.forEach(Animation::stop);
        motions.forEach(Animation::stop);
    }

    // ---------- colour blending helpers ----------

    private static LinearGradient verticalMix(Color[] a, Color[] b, double[] pos, double t) {
        Stop[] stops = new Stop[a.length];
        for (int i = 0; i < a.length; i++) {
            stops[i] = new Stop(pos[i], mix(a[i], b[i], t));
        }
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
    }

    private static Color mix(Color a, Color b, double t) {
        return Color.color(
                lerp(a.getRed(), b.getRed(), t),
                lerp(a.getGreen(), b.getGreen(), t),
                lerp(a.getBlue(), b.getBlue(), t),
                lerp(a.getOpacity(), b.getOpacity(), t));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smooth(double x) {
        double c = Math.max(0, Math.min(1, x));
        return c * c * (3 - 2 * c);
    }
}

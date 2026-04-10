package com.whackawhack.whackawhack;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WhackController {

    @FXML private Label scoreLabel;
    @FXML private Label timerLabel;
    @FXML private ImageView mrBossImageView;

    @FXML private ImageView hole1, hole2, hole3;
    @FXML private ImageView hole4, hole5, hole6;
    @FXML private ImageView hole7, hole8, hole9;
    @FXML private ImageView jumpscareView;

    private MediaPlayer ostPlayer;
    private Media bonkMedia;
    private Media kingLaughMedia;
    private Media blowMedia;
    private Media jumpscareMedia;

    // Atomic Variables - lock-free CAS operations
    private final AtomicInteger score = new AtomicInteger(0);
    private final AtomicInteger timeLeft = new AtomicInteger(60);
    private final AtomicInteger scaryHitCount = new AtomicInteger(0);

    private Image holeImage, normalMoleImage, goldenMoleImage,
            explosiveMoleImage, scaryMoleImage;
    private Image catHappyImage, catChillImage, catScaryImage, jumpscareImage;

    private ImageView[] holes;
    private final Random random = new Random();
    private ScheduledExecutorService scheduler;

    @FXML
    public void initialize() {
        // Load images
        holeImage          = loadImage("hole.png");
        normalMoleImage    = loadImage("normalMole.png");
        goldenMoleImage    = loadImage("goldenMole.png");
        explosiveMoleImage = loadImage("explosiveMole.png");
        scaryMoleImage     = loadImage("scaryMole.png");
        catHappyImage      = loadImage("cat1.png");
        catChillImage      = loadImage("cat2.png");
        catScaryImage      = loadImage("cat3.png");
        jumpscareImage     = loadImage("jumpscare.png");

        // Load sounds
        ostPlayer      = new MediaPlayer(new Media(getClass().getResource("/com/whackawhack/whackawhack/ost.mp3").toString()));
        bonkMedia      = new Media(getClass().getResource("/com/whackawhack/whackawhack/Bonk.mp3").toString());
        kingLaughMedia = new Media(getClass().getResource("/com/whackawhack/whackawhack/kingLaugh.mp3").toString());
        blowMedia      = new Media(getClass().getResource("/com/whackawhack/whackawhack/blow.mp3").toString());
        jumpscareMedia = new Media(getClass().getResource("/com/whackawhack/whackawhack/JUMPSCAREv2.mp3").toString());

        // Loop OST
        if (ostPlayer != null) {
            ostPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            ostPlayer.play();
        }

        // Set initial boss image
        mrBossImageView.setImage(catHappyImage);

        // Setup holes
        holes = new ImageView[]{hole1, hole2, hole3, hole4, hole5, hole6, hole7, hole8, hole9};
        for (int i = 0; i < holes.length; i++) {
            final int index = i;
            holes[i].setImage(holeImage);
            holes[i].setOnMouseClicked(e -> handleWhack(index));
        }

        startGame();
    }

    private Image loadImage(String name) {
        var stream = getClass().getResourceAsStream("/com/whackawhack/whackawhack/" + name);
        if (stream == null) System.out.println("❌ Missing image: " + name);
        return new Image(stream);
    }

    private void playSound(Media media) {
        if (media != null) {
            MediaPlayer player = new MediaPlayer(media);
            player.play();
        }
    }

    private void startGame() {
        // Reset atomic variables
        score.set(0);
        timeLeft.set(60);
        scaryHitCount.set(0);

        scoreLabel.setText("0");
        timerLabel.setText("60");

        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::showRandomMole, 0, 1200, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::tickTimer, 1, 1, TimeUnit.SECONDS);
    }

    private void showRandomMole() {
        int holeIndex = random.nextInt(9);
        Image mole = pickRandomMole();

        Platform.runLater(() -> holes[holeIndex].setImage(mole));

        scheduler.schedule(() -> {
            Platform.runLater(() -> {
                if (holes[holeIndex].getImage() != holeImage) {
                    holes[holeIndex].setImage(holeImage);
                }
            });
        }, 1000, TimeUnit.MILLISECONDS);
    }

    private Image pickRandomMole() {
        return switch (random.nextInt(4)) {
            case 0 -> normalMoleImage;
            case 1 -> goldenMoleImage;
            case 2 -> explosiveMoleImage;
            default -> scaryMoleImage;
        };
    }

    private void handleWhack(int index) {
        Image current = holes[index].getImage();

        if (current == normalMoleImage) {
            // CAS - atomically add 1
            score.addAndGet(1);
            playSound(bonkMedia);
        } else if (current == goldenMoleImage) {
            // CAS - atomically add 3
            score.addAndGet(3);
            playSound(kingLaughMedia);
        } else if (current == explosiveMoleImage) {
            // CAS - atomically subtract 2
            score.addAndGet(-2);
            playSound(blowMedia);
        } else if (current == scaryMoleImage) {
            handleScaryHit(index);
            return;
        } else {
            return;
        }

        Platform.runLater(() -> {
            holes[index].setImage(holeImage);
            // get() reads the atomic value safely across threads
            scoreLabel.setText(String.valueOf(score.get()));
        });
    }

    private void handleScaryHit(int index) {
        Platform.runLater(() -> holes[index].setImage(holeImage));

        // incrementAndGet - atomic CAS increment
        int hits = scaryHitCount.incrementAndGet();

        Platform.runLater(() -> {
            switch (hits) {
                case 1 -> mrBossImageView.setImage(catChillImage);
                case 2 -> mrBossImageView.setImage(catScaryImage);
                case 3 -> triggerJumpscare();
            }
        });
    }

    private void triggerJumpscare() {
        playSound(jumpscareMedia);
        if (ostPlayer != null) ostPlayer.stop();
        scheduler.shutdownNow();

        Platform.runLater(() -> {
            jumpscareView.setImage(jumpscareImage);
            jumpscareView.setVisible(true);
        });

        ScheduledExecutorService closeScheduler = Executors.newSingleThreadScheduledExecutor();
        closeScheduler.schedule(() -> {
            Platform.runLater(Platform::exit);
            closeScheduler.shutdown();
        }, 2, TimeUnit.SECONDS);
    }

    private void tickTimer() {
        // decrementAndGet - atomic CAS decrement
        int remaining = timeLeft.decrementAndGet();
        Platform.runLater(() -> timerLabel.setText(String.valueOf(remaining)));

        if (remaining <= 0) {
            scheduler.shutdownNow();
            if (ostPlayer != null) ostPlayer.stop();
            Platform.runLater(this::endGame);
        }
    }

    private void endGame() {
        for (ImageView hole : holes) hole.setImage(holeImage);
        scoreLabel.setText(String.valueOf(score.get()));
        timerLabel.setText("0");
    }
}

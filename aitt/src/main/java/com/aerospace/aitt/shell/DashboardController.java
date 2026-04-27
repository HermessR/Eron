package com.aerospace.aitt.shell;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospace.aitt.core.auth.AuthService;
import com.aerospace.aitt.core.auth.User;
import com.aerospace.aitt.core.ui.Alerts;
import com.aerospace.aitt.core.ui.BaseController;
import com.aerospace.aitt.core.ui.StageManager;
import com.aerospace.aitt.core.ui.ThemeManager;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controller for the main dashboard view.
 * Displays module selection with carousel effects on module cards.
 */
@SuppressWarnings("unused") // FXML handlers and fields are invoked via reflection
public class DashboardController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Carousel effect constants
    private static final double CENTER_SCALE = 1.05;
    private static final double SIDE_SCALE = 0.9;
    private static final double FAR_SCALE = 0.8;
    private static final double CENTER_OPACITY = 1.0;
    private static final double SIDE_OPACITY = 0.6;
    private static final double FAR_OPACITY = 0.35;
    private static final double SIDE_BLUR = 2.0;
    private static final double FAR_BLUR = 4.0;
    private static final double ANIMATION_MS = 250;
    
    @FXML private Label lblWelcome;
    @FXML private Label lblDateTime;
    @FXML private Label userDisplayName;
    @FXML private Label userRole;
    @FXML private Button flashingModuleBtn;
    @FXML private Button configModuleBtn;
    @FXML private Button testDevModuleBtn;
    @FXML private VBox moduleContainer;
    
    // Module cards
    @FXML private HBox moduleCardsContainer;
    @FXML private VBox cardFlashing;
    @FXML private VBox cardHardware;
    @FXML private VBox cardScripting;
    @FXML private VBox cardConfig;
    @FXML private VBox cardRemote;
    @FXML private VBox cardTestDev;
    
    private List<VBox> moduleCards;
    private int selectedIndex = 0;
    private volatile boolean isAnimating = false;
    
    private final AuthService authService = AuthService.getInstance();
    private Timeline clockTimeline;
    private AudioClip navigationSound;
    
    @Override
    @FXML
    public void initialize() {
        log.debug("Initializing DashboardController");
        setupClock();
        setupNavigationSound();
        setupModuleCarousel();
    }
    
    /**
     * Initializes the navigation sound effect.
     */
    private void setupNavigationSound() {
        try {
            var soundUrl = getClass().getResource("/sounds/ding.wav");
            if (soundUrl != null) {
                navigationSound = new AudioClip(soundUrl.toExternalForm());
                navigationSound.setVolume(0.5); // 50% volume
                log.debug("Navigation sound loaded");
            } else {
                log.warn("Navigation sound file not found at /sounds/ding.wav");
            }
        } catch (Exception e) {
            log.warn("Failed to load navigation sound: {}", e.getMessage());
        }
    }
    
    /**
     * Plays the navigation sound effect.
     */
    private void playNavigationSound() {
        if (navigationSound != null) {
            navigationSound.play();
        }
    }
    
    /**
     * Sets up the module cards carousel with visual effects.
     */
    private void setupModuleCarousel() {
        // Collect module cards
        moduleCards = new ArrayList<>();
        if (cardFlashing != null) moduleCards.add(cardFlashing);
        if (cardHardware != null) moduleCards.add(cardHardware);
        if (cardScripting != null) moduleCards.add(cardScripting);
        if (cardConfig != null) moduleCards.add(cardConfig);
        if (cardRemote != null) moduleCards.add(cardRemote);
        if (cardTestDev != null) moduleCards.add(cardTestDev);
        
        if (moduleCards.isEmpty()) {
            log.warn("No module cards found in FXML");
            return;
        }
        
        // Start with center card (index 1 = Hardware)
        selectedIndex = 1;
        
        // Apply initial effects
        updateCarouselEffects(false);
        
        // Setup keyboard navigation
        if (moduleCardsContainer != null) {
            moduleCardsContainer.setFocusTraversable(true);
            moduleCardsContainer.setOnKeyPressed(e -> {
                if (isAnimating) return; // Prevent action during animation
                if (e.getCode() == KeyCode.LEFT) {
                    navigatePrevious();
                    e.consume();
                } else if (e.getCode() == KeyCode.RIGHT) {
                    navigateNext();
                    e.consume();
                } else if (e.getCode() == KeyCode.ENTER) {
                    openSelectedModule();
                    e.consume();
                }
            });
            
            // Mouse scroll navigation
            moduleCardsContainer.setOnScroll(this::handleScroll);
        }
        
        // Add click handlers for selection
        for (int i = 0; i < moduleCards.size(); i++) {
            final int index = i;
            VBox card = moduleCards.get(i);
            card.setOnMouseClicked(e -> {
                if (isAnimating) return; // Prevent action during animation
                if (index == selectedIndex) {
                    // Open if already selected
                    openModuleAtIndex(index);
                } else {
                    selectCard(index);
                }
            });
        }
        
        log.debug("Module carousel initialized with {} cards", moduleCards.size());
    }
    
    /**
     * Handles mouse scroll for navigation.
     */
    private void handleScroll(ScrollEvent e) {
        if (isAnimating) return;
        
        if (e.getDeltaY() > 0 || e.getDeltaX() > 0) {
            navigatePrevious();
        } else if (e.getDeltaY() < 0 || e.getDeltaX() < 0) {
            navigateNext();
        }
        e.consume();
    }
    
    /**
     * Navigates to the next module card.
     */
    private void navigateNext() {
        if (isAnimating || moduleCards.isEmpty()) return;
        int nextIndex = (selectedIndex + 1) % moduleCards.size();
        selectCard(nextIndex);
    }
    
    /**
     * Navigates to the previous module card.
     */
    private void navigatePrevious() {
        if (isAnimating || moduleCards.isEmpty()) return;
        int prevIndex = (selectedIndex - 1 + moduleCards.size()) % moduleCards.size();
        selectCard(prevIndex);
    }
    
    /**
     * Selects a card by index with animation.
     */
    private void selectCard(int index) {
        if (index == selectedIndex || isAnimating) return;
        selectedIndex = index;
        playNavigationSound();
        updateCarouselEffects(true);
    }
    
    /**
     * Updates visual effects on all cards based on distance from selected.
     * Reorders cards in HBox to create infinite circular carousel effect.
     */
    private void updateCarouselEffects(boolean animate) {
        if (moduleCards.isEmpty() || moduleCardsContainer == null) return;
        
        if (animate) {
            isAnimating = true;
        }
        
        int size = moduleCards.size();
        int center = size / 2; // Visual center position
        
        // Reorder cards so selected is visually centered
        // Build list with selected card at center position
        List<VBox> orderedCards = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            // Calculate which card goes at visual position i
            // At position 'center', we want 'selectedIndex'
            int offset = i - center;
            int cardIdx = (selectedIndex + offset + size) % size;
            orderedCards.add(moduleCards.get(cardIdx));
        }
        
        // Reorder children in container
        moduleCardsContainer.getChildren().setAll(orderedCards);
        
        // Single timeline for all cards
        Timeline timeline = animate ? new Timeline() : null;
        
        for (int visualPos = 0; visualPos < orderedCards.size(); visualPos++) {
            VBox card = orderedCards.get(visualPos);
            // Distance from visual center
            int distance = Math.abs(visualPos - center);
            
            double targetScale;
            double targetOpacity;
            double blurRadius;
            
            if (distance == 0) {
                // Center (selected) card
                targetScale = CENTER_SCALE;
                targetOpacity = CENTER_OPACITY;
                blurRadius = 0;
            } else if (distance == 1) {
                // Adjacent cards
                targetScale = SIDE_SCALE;
                targetOpacity = SIDE_OPACITY;
                blurRadius = SIDE_BLUR;
            } else {
                // Far cards
                targetScale = FAR_SCALE;
                targetOpacity = FAR_OPACITY;
                blurRadius = FAR_BLUR;
            }
            
            if (animate && timeline != null) {
                // Add keyvalues to single timeline
                KeyValue kvScaleX = new KeyValue(card.scaleXProperty(), targetScale, Interpolator.EASE_BOTH);
                KeyValue kvScaleY = new KeyValue(card.scaleYProperty(), targetScale, Interpolator.EASE_BOTH);
                KeyValue kvOpacity = new KeyValue(card.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH);
                timeline.getKeyFrames().add(new KeyFrame(Duration.millis(ANIMATION_MS), kvScaleX, kvScaleY, kvOpacity));
                
                // Apply blur immediately
                card.setEffect(blurRadius > 0 ? new GaussianBlur(blurRadius) : null);
            } else {
                applyCardEffects(card, targetScale, targetOpacity, blurRadius);
            }
            
            // Update style class
            card.getStyleClass().remove("module-card-selected");
            if (distance == 0) {
                card.getStyleClass().add("module-card-selected");
            }
        }
        
        // Play single timeline for all cards
        if (timeline != null && !timeline.getKeyFrames().isEmpty()) {
            timeline.setOnFinished(e -> isAnimating = false);
            timeline.play();
        } else if (animate) {
            isAnimating = false;
        }
    }
    
    /**
     * Applies effects immediately without animation.
     */
    private void applyCardEffects(VBox card, double scale, double opacity, double blurRadius) {
        card.setScaleX(scale);
        card.setScaleY(scale);
        card.setOpacity(opacity);
        card.setEffect(blurRadius > 0 ? new GaussianBlur(blurRadius) : null);
    }
    
    /**
     * Opens the currently selected module.
     */
    private void openSelectedModule() {
        openModuleAtIndex(selectedIndex);
    }
    
    /**
     * Opens module at specified index.
     */
    private void openModuleAtIndex(int index) {
        switch (index) {
            case 0 -> openFlashingModule();
            case 1 -> openHardwareModule();
            case 2 -> openScriptingModule();
            case 3 -> openConfigModule();
            case 4 -> openRemoteModule();
            case 5 -> openTestDevModule();
        }
    }
    
    /**
     * Sets up the real-time clock display.
     */
    private void setupClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateDateTime()));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        updateDateTime(); // Initial update
    }
    
    /**
     * Updates the date/time label with current system time.
     */
    private void updateDateTime() {
        if (lblDateTime != null) {
            lblDateTime.setText(LocalDateTime.now().format(DATE_TIME_FORMAT));
        }
    }
    
    @Override
    public void onShow() {
        // Update user info
        authService.getCurrentUser().ifPresent(this::updateUserInfo);
        
        // Start the clock
        if (clockTimeline != null) {
            clockTimeline.play();
        }
        
        // Focus the carousel for keyboard navigation
        if (moduleCardsContainer != null) {
            moduleCardsContainer.requestFocus();
        }
        
        log.info("Dashboard displayed");
    }
    
    @Override
    public void onHide() {
        // Stop the clock when leaving dashboard
        if (clockTimeline != null) {
            clockTimeline.stop();
        }
    }
    
    /**
     * Updates the user info display.
     */
    private void updateUserInfo(User user) {
        if (userDisplayName != null) {
            userDisplayName.setText(user.displayName());
        }
        if (userRole != null) {
            userRole.setText(user.role().name());
        }
    }
    
    /**
     * Opens the Flashing Module.
     */
    @FXML
    private void openFlashingModule() {
        log.info("Opening Flashing Module");
        StageManager.getInstance().switchScene("/fxml/flashing.fxml");
    }
    
    /**
     * Opens the Flashing Module (handler for FXML).
     */
    @FXML
    private void handleFlashingModule(MouseEvent event) {
        if (selectedIndex == 0) {
            openFlashingModule();
        } else {
            selectCard(0);
        }
    }
    
    /**
     * Opens the Configuration Module.
     */
    @FXML
    private void openConfigModule() {
        log.info("Opening Configuration Module");
        StageManager.getInstance().switchScene("/fxml/config.fxml");
    }

    /**
     * Opens the Configuration Module (handler for FXML).
     */
    @FXML
    private void handleConfigModule(MouseEvent event) {
        if (selectedIndex == 3) {
            openConfigModule();
        } else {
            selectCard(3);
        }
    }

    /**
     * Opens the Remote Probes Module.
     */
    @FXML
    private void openRemoteModule() {
        log.info("Opening Remote Probes Module");
        StageManager.getInstance().switchScene("/fxml/remote.fxml");
    }

    /**
     * Opens the Remote Probes Module (handler for FXML).
     */
    @FXML
    private void handleRemoteModule(MouseEvent event) {
        if (selectedIndex == 4) {
            openRemoteModule();
        } else {
            selectCard(4);
        }
    }
    
    /**
     * Opens the Test Development Module.
     */
    @FXML
    private void openTestDevModule() {
        log.info("Opening Test Development Module");
        StageManager.getInstance().switchScene("/fxml/testdev.fxml");
    }
    
    /**
     * Opens the Test Development Module (handler for FXML).
     */
    @FXML
    private void handleTestDevModule(MouseEvent event) {
        if (selectedIndex == 5) {
            openTestDevModule();
        } else {
            selectCard(5);
        }
    }
    
    /**
     * Opens the Hardware Module.
     */
    @FXML
    private void openHardwareModule() {
        log.info("Opening Hardware Module");
        StageManager.getInstance().switchScene("/fxml/hardware.fxml");
    }
    
    /**
     * Opens the Hardware Module (handler for FXML).
     */
    @FXML
    private void handleHardwareModule(MouseEvent event) {
        if (selectedIndex == 1) {
            openHardwareModule();
        } else {
            selectCard(1);
        }
    }
    
    /**
     * Opens the Scripting Console.
     */
    @FXML
    private void openScriptingModule() {
        log.info("Opening Scripting Console");
        StageManager.getInstance().switchScene("/fxml/scripting.fxml");
    }
    
    /**
     * Opens the Scripting Console (handler for FXML).
     */
    @FXML
    private void handleScriptingModule(MouseEvent event) {
        if (selectedIndex == 2) {
            openScriptingModule();
        } else {
            selectCard(2);
        }
    }
    
    /**
     * Handles user logout.
     */
    @FXML
    private void handleLogout() {
        if (Alerts.confirm("Logout", "Are you sure you want to logout?")) {
            authService.logout();
            StageManager.getInstance().switchScene("/fxml/login.fxml");
            log.info("User logged out");
        }
    }
    
    /**
     * Opens settings dialog.
     */
    @FXML
    private void openSettings() {
        log.info("Opening Settings dialog");
        try {
            java.net.URL fxmlResource = getClass().getResource("/fxml/settings.fxml");
            if (fxmlResource == null) {
                log.error("Settings FXML resource not found");
                Alerts.error("Error", "Settings dialog resource not found");
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlResource);
            Parent root = loader.load();
            
            if (root == null) {
                log.error("Failed to load settings FXML - loader returned null");
                Alerts.error("Error", "Failed to load settings dialog");
                return;
            }
            
            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(StageManager.getInstance().getPrimaryStage());
            
            Scene scene = new Scene(root, 700, 550);
            ThemeManager.getInstance().applyCurrentTheme(scene);
            settingsStage.setScene(scene);
            settingsStage.setResizable(false);
            settingsStage.showAndWait();
        } catch (IOException e) {
            log.error("Failed to open settings dialog: {}", e.getMessage());
            Alerts.error("Error", "Failed to open settings: " + e.getMessage());
        }
    }
}

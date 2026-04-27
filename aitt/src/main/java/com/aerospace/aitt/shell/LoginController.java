package com.aerospace.aitt.shell;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospace.aitt.core.auth.AuthService;
import com.aerospace.aitt.core.ui.BaseController;
import com.aerospace.aitt.core.ui.StageManager;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

/**
 * Controller for the login view.
 * Handles user authentication and navigation to dashboard.
 */
@SuppressWarnings("unused") // FXML handlers and fields are invoked via reflection
public class LoginController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    
    @FXML private TextField txtLicenseKey;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblAppTitle;
    @FXML private Label lblStatus;
    @FXML private CheckBox chkRemember;
    
    private final AuthService authService = AuthService.getInstance();
    private final DoubleProperty titleFillLevel = new SimpleDoubleProperty(0.2);
    private final DoubleProperty titleWavePhase = new SimpleDoubleProperty(0.0);
    private Timeline titleFillTimeline;
    private Timeline titleWaveTimeline;
    
    @Override
    @FXML
    public void initialize() {
        log.debug("Initializing LoginController");
        
        // Hide status initially
        if (lblStatus != null) {
            lblStatus.setVisible(false);
        }
        
        // Setup enter key handling
        if (txtUsername != null) {
            txtUsername.setOnKeyPressed(this::handleKeyPress);
        }
        if (txtPassword != null) {
            txtPassword.setOnKeyPressed(this::handleKeyPress);
        }
        if (txtLicenseKey != null) {
            txtLicenseKey.setOnKeyPressed(this::handleKeyPress);
        }

        // Start animated water-fill effect for app title
        initializeTitleAnimation();
        
        // Focus license key field on load
        Platform.runLater(() -> {
            if (txtLicenseKey != null) {
                txtLicenseKey.requestFocus();
            }
        });
    }
    
    @Override
    public void onShow() {
        // Clear fields when showing login
        if (txtUsername != null) txtUsername.clear();
        if (txtPassword != null) txtPassword.clear();
        if (txtLicenseKey != null) txtLicenseKey.clear();
        if (lblStatus != null) lblStatus.setVisible(false);
        startTitleAnimation();
    }

    @Override
    public void onHide() {
        stopTitleAnimation();
    }

    @Override
    public void onClose() {
        stopTitleAnimation();
    }
    
    /**
     * Handles the login button click.
     */
    @FXML
    private void handleLogin() {
        String licenseKey = txtLicenseKey != null ? txtLicenseKey.getText().trim() : "";
        String username = txtUsername != null ? txtUsername.getText().trim() : "";
        String password = txtPassword != null ? txtPassword.getText() : "";
        
        // Validate inputs
        if (licenseKey.isEmpty()) {
            showError("License key is required");
            if (txtLicenseKey != null) txtLicenseKey.requestFocus();
            return;
        }
        
        if (username.isEmpty()) {
            showError("Username is required");
            if (txtUsername != null) txtUsername.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showError("Password is required");
            if (txtPassword != null) txtPassword.requestFocus();
            return;
        }
        
        // Show loading state
        setLoading(true);
        hideError();
        
        // Perform authentication
        runAsync(
            () -> authService.authenticate(username, password),
            result -> {
                setLoading(false);
                
                if (result.success()) {
                    log.info("Login successful for user: {}", username);
                    navigateToDashboard();
                } else {
                    log.warn("Login failed for user: {}", username);
                    showError(result.errorMessage());
                    if (txtPassword != null) {
                        txtPassword.clear();
                        txtPassword.requestFocus();
                    }
                }
            }
        );
    }
    
    /**
     * Handles key press events for enter key login.
     */
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleLogin();
        }
    }
    
    /**
     * Navigates to the dashboard.
     */
    private void navigateToDashboard() {
        StageManager.getInstance().switchScene("/fxml/dashboard.fxml");
    }
    
    /**
     * Shows an error message.
     */
    private void showError(String message) {
        if (lblStatus != null) {
            lblStatus.setText(message);
            lblStatus.setVisible(true);
            lblStatus.setStyle("-fx-text-fill: #ff4444;");
        }
    }
    
    /**
     * Hides the error message.
     */
    private void hideError() {
        if (lblStatus != null) {
            lblStatus.setVisible(false);
        }
    }
    
    /**
     * Sets the loading state.
     */
    private void setLoading(boolean loading) {
        if (btnLogin != null) {
            btnLogin.setDisable(loading);
            btnLogin.setText(loading ? "Logging in..." : "Login");
        }
        if (txtUsername != null) {
            txtUsername.setDisable(loading);
        }
        if (txtPassword != null) {
            txtPassword.setDisable(loading);
        }
        if (txtLicenseKey != null) {
            txtLicenseKey.setDisable(loading);
        }
    }

    /**
     * Initializes a looped water-fill animation for the login app title.
     */
    private void initializeTitleAnimation() {
        if (lblAppTitle == null) {
            return;
        }

        titleFillLevel.addListener((obs, oldValue, newValue) -> applyTitleWaterFillStyle());
        titleWavePhase.addListener((obs, oldValue, newValue) -> applyTitleWaterFillStyle());

        titleFillTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(titleFillLevel, 0.18, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.seconds(2.8),
                new KeyValue(titleFillLevel, 0.9, Interpolator.EASE_BOTH))
        );
        titleFillTimeline.setCycleCount(Animation.INDEFINITE);
        titleFillTimeline.setAutoReverse(true);

        titleWaveTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(titleWavePhase, 0.0, Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(1.6),
                new KeyValue(titleWavePhase, Math.PI * 2.0, Interpolator.LINEAR))
        );
        titleWaveTimeline.setCycleCount(Animation.INDEFINITE);

        applyTitleWaterFillStyle();
        startTitleAnimation();
    }

    /**
     * Starts title animation timelines when the login view is visible.
     */
    private void startTitleAnimation() {
        if (titleFillTimeline != null && titleFillTimeline.getStatus() != Animation.Status.RUNNING) {
            titleFillTimeline.play();
        }
        if (titleWaveTimeline != null && titleWaveTimeline.getStatus() != Animation.Status.RUNNING) {
            titleWaveTimeline.play();
        }
    }

    /**
     * Stops title animation timelines to avoid running while off-screen.
     */
    private void stopTitleAnimation() {
        if (titleFillTimeline != null) {
            titleFillTimeline.stop();
        }
        if (titleWaveTimeline != null) {
            titleWaveTimeline.stop();
        }
    }

    /**
     * Applies a dynamic gradient that mimics a water surface filling the title.
     */
    private void applyTitleWaterFillStyle() {
        if (lblAppTitle == null) {
            return;
        }

        double fill = clamp(titleFillLevel.get(), 0.08, 0.95);
        double fillPercent = fill * 100.0;
        double waveOffset = Math.sin(titleWavePhase.get()) * 3.5;

        double lowerEdge = clamp(fillPercent - 7.0 + waveOffset, 0.0, 100.0);
        double waveEdge = clamp(fillPercent + waveOffset, 0.0, 100.0);
        double upperEdge = clamp(fillPercent + 4.0 + waveOffset, 0.0, 100.0);

        lblAppTitle.setStyle(String.format(Locale.US,
            "-fx-text-fill: linear-gradient(from 0%% 100%% to 0%% 0%%, " +
            "-aitt-accent-primary 0%%, " +
            "-aitt-accent-primary %.2f%%, " +
            "-aitt-accent-hover %.2f%%, " +
            "-aitt-text-primary %.2f%%, " +
            "-aitt-text-primary 100%%);",
            lowerEdge,
            waveEdge,
            upperEdge
        ));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

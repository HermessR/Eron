package com.aerospace.aitt.core.ui;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Manages application stages and scene navigation.
 * Uses a single persistent scene with swappable content to prevent window resizing.
 */
public class StageManager {
    
    private static final Logger log = LoggerFactory.getLogger(StageManager.class);
    private static StageManager instance;
    
    private Stage primaryStage;
    private Scene mainScene;
    private StackPane rootContainer;
    private final Map<String, ViewData> viewCache = new HashMap<>();
    private BaseController currentController;
    
    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 720;
    
    private StageManager() {}
    
    public static synchronized StageManager getInstance() {
        if (instance == null) {
            instance = new StageManager();
        }
        return instance;
    }
    
    /**
     * Initializes the stage manager with the primary stage.
     */
    public void init(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Automation Integration Test Tool");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(600);
        primaryStage.setResizable(true);
        
        // Create a single persistent scene with a container
        rootContainer = new StackPane();
        mainScene = new Scene(rootContainer, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        
        // Apply theme from settings
        ThemeManager.getInstance().applyCurrentTheme(mainScene);
        
        primaryStage.setScene(mainScene);
    }
    
    /**
     * Returns the primary stage.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * Navigates to a view by loading its FXML.
     * The window size remains unchanged - only the content is swapped.
     */
    public void switchScene(String fxmlPath) {
        try {
            // Check cache first
            ViewData viewData = viewCache.get(fxmlPath);
            
            if (viewData == null) {
                viewData = loadView(fxmlPath);
                viewCache.put(fxmlPath, viewData);
            }
            
            // Notify old controller
            if (currentController != null) {
                currentController.onHide();
            }
            
            // Swap content in the container (window size stays the same)
            rootContainer.getChildren().setAll(viewData.root);
            currentController = viewData.controller;
            
            // Initialize new controller
            if (currentController != null) {
                currentController.setStage(primaryStage);
                currentController.onShow();
            }
            
            // Show stage only if not already showing
            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
            
            log.info("Switched to scene: {}", fxmlPath);
            
        } catch (IOException e) {
            log.error("Failed to load scene: {}", fxmlPath, e);
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }
    
    /**
     * Loads a view from FXML.
     */
    private ViewData loadView(String fxmlPath) throws IOException {
        URL fxmlResource = getClass().getResource(fxmlPath);
        if (fxmlResource == null) {
            throw new IOException("FXML resource not found: " + fxmlPath);
        }
        FXMLLoader loader = new FXMLLoader(fxmlResource);
        Parent root = loader.load();
        
        // Ensure the view expands to fill the container
        if (root instanceof Region region) {
            region.setMinSize(0, 0);
            region.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        
        BaseController controller = loader.getController();
        return new ViewData(root, controller);
    }
    
    /**
     * Legacy method for compatibility - loads a scene (returns SceneData).
     */
    public SceneData loadScene(String fxmlPath) throws IOException {
        URL fxmlResource = getClass().getResource(fxmlPath);
        if (fxmlResource == null) {
            throw new IOException("FXML resource not found: " + fxmlPath);
        }
        FXMLLoader loader = new FXMLLoader(fxmlResource);
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        URL cssResource = getClass().getResource("/css/style.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        } else {
            log.warn("Could not load CSS stylesheet for scene");
        }
        
        BaseController controller = loader.getController();
        return new SceneData(scene, controller);
    }
    
    /**
     * Clears the view cache.
     */
    public void clearCache() {
        viewCache.clear();
    }
    
    /**
     * Clears a specific view from cache.
     */
    public void clearScene(String fxmlPath) {
        viewCache.remove(fxmlPath);
    }
    
    /**
     * Returns the current controller.
     */
    public BaseController getCurrentController() {
        return currentController;
    }
    
    /**
     * Data class holding scene and controller (legacy).
     */
    public record SceneData(Scene scene, BaseController controller) {}
    
    /**
     * Data class holding view root and controller.
     */
    private record ViewData(Parent root, BaseController controller) {}
}

package com.aerospace.aitt.core.ui;

import java.util.List;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Demo application showcasing the InfiniteCarousel component.
 * 
 * <p>Run this class to see the carousel in action with sample options.</p>
 */
public class InfiniteCarouselDemo extends Application {
    
    private Label selectedLabel;
    
    @Override
    public void start(Stage primaryStage) {
        // Create the carousel
        InfiniteCarousel carousel = new InfiniteCarousel();
        
        // Set sample items
        List<String> options = List.of(
            "Dashboard",
            "Flashing",
            "Hardware",
            "Scripting",
            "Configuration",
            "Test Dev"
        );
        carousel.setItems(options);
        
        // Selection label
        selectedLabel = new Label("Selected: " + carousel.getSelectedItem());
        selectedLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Callback when selection changes
        carousel.setOnSelectionChanged(item -> {
            selectedLabel.setText("Selected: " + item);
        });
        
        // Navigation buttons
        Button prevBtn = new Button("◀ Previous");
        prevBtn.setOnAction(e -> carousel.previous());
        prevBtn.getStyleClass().add("nav-button");
        
        Button nextBtn = new Button("Next ▶");
        nextBtn.setOnAction(e -> carousel.next());
        nextBtn.getStyleClass().add("nav-button");
        
        HBox navButtons = new HBox(20, prevBtn, nextBtn);
        navButtons.setAlignment(Pos.CENTER);
        
        // Title
        Label title = new Label("Infinite Carousel Demo");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        Label instructions = new Label("Use arrow keys, mouse scroll, or buttons to navigate");
        instructions.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        
        VBox header = new VBox(10, title, instructions);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));
        
        // Center content
        VBox centerContent = new VBox(30, carousel, selectedLabel, navButtons);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(50));
        
        // Main layout
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(centerContent);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");
        
        // Scene
        Scene scene = new Scene(root, 800, 500);
        scene.getStylesheets().add(getClass().getResource("/css/carousel-demo.css").toExternalForm());
        
        // Request focus for keyboard navigation
        scene.setOnMouseClicked(e -> carousel.requestFocus());
        carousel.requestFocus();
        
        primaryStage.setTitle("Infinite Carousel Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

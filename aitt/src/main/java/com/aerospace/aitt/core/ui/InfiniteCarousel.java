package com.aerospace.aitt.core.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * A horizontal infinite carousel slider component for JavaFX.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Infinite looping navigation</li>
 *   <li>Smooth sliding animations</li>
 *   <li>Center item highlighting with scale, opacity, and blur effects</li>
 *   <li>Keyboard navigation (arrow keys)</li>
 *   <li>Mouse scroll support</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <pre>{@code
 * InfiniteCarousel carousel = new InfiniteCarousel();
 * carousel.setItems(List.of("Option 1", "Option 2", "Option 3"));
 * carousel.setOnSelectionChanged(item -> System.out.println("Selected: " + item));
 * }</pre>
 */
public class InfiniteCarousel extends StackPane {
    
    // ==================== CONFIGURATION ====================
    
    /** Animation duration in milliseconds */
    private static final double ANIMATION_DURATION_MS = 300;
    
    /** Scale for the center (focused) item */
    private static final double CENTER_SCALE = 1.2;
    
    /** Scale for side items */
    private static final double SIDE_SCALE = 0.85;
    
    /** Opacity for the center item */
    private static final double CENTER_OPACITY = 1.0;
    
    /** Opacity for side items */
    private static final double SIDE_OPACITY = 0.5;
    
    /** Blur radius for side items */
    private static final double SIDE_BLUR_RADIUS = 3.0;
    
    /** Spacing between items */
    private static final double ITEM_SPACING = 40;
    
    /** Number of visible items on each side of center */
    private static final int VISIBLE_SIDE_ITEMS = 2;
    
    // ==================== STATE ====================
    
    private final HBox itemContainer;
    private final List<Label> itemLabels = new ArrayList<>();
    private List<String> items = new ArrayList<>();
    
    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(0);
    private boolean isAnimating = false;
    private Consumer<String> onSelectionChanged;
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Creates a new InfiniteCarousel with default settings.
     */
    public InfiniteCarousel() {
        itemContainer = new HBox(ITEM_SPACING);
        itemContainer.setAlignment(Pos.CENTER);
        
        getChildren().add(itemContainer);
        setAlignment(Pos.CENTER);
        
        // Enable focus for keyboard events
        setFocusTraversable(true);
        
        // Setup event handlers
        setupKeyboardNavigation();
        setupMouseScrollNavigation();
        
        // Apply default styling
        getStyleClass().add("infinite-carousel");
        
        // Listen to selection changes
        selectedIndex.addListener((obs, oldVal, newVal) -> {
            updateVisualEffects();
            if (onSelectionChanged != null && !items.isEmpty()) {
                onSelectionChanged.accept(items.get(newVal.intValue()));
            }
        });
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Sets the list of items to display in the carousel.
     * 
     * @param items List of string options
     */
    public void setItems(List<String> items) {
        if (items == null || items.isEmpty()) {
            this.items = new ArrayList<>();
            itemLabels.clear();
            itemContainer.getChildren().clear();
            return;
        }
        
        this.items = new ArrayList<>(items);
        selectedIndex.set(0);
        rebuildCarousel();
    }
    
    /**
     * Returns the currently selected item.
     * 
     * @return The selected item string, or null if no items
     */
    public String getSelectedItem() {
        if (items.isEmpty()) {
            return null;
        }
        return items.get(selectedIndex.get());
    }
    
    /**
     * Returns the index of the currently selected item.
     * 
     * @return The selected index
     */
    public int getSelectedIndex() {
        return selectedIndex.get();
    }
    
    /**
     * Sets the selected index.
     * 
     * @param index The index to select
     */
    public void setSelectedIndex(int index) {
        if (items.isEmpty()) return;
        int normalizedIndex = normalizeIndex(index);
        if (normalizedIndex != selectedIndex.get()) {
            animateToIndex(normalizedIndex);
        }
    }
    
    /**
     * Navigates to the next item with animation.
     */
    public void next() {
        if (items.isEmpty() || isAnimating) return;
        int nextIndex = normalizeIndex(selectedIndex.get() + 1);
        animateTransition(1, nextIndex);
    }
    
    /**
     * Navigates to the previous item with animation.
     */
    public void previous() {
        if (items.isEmpty() || isAnimating) return;
        int prevIndex = normalizeIndex(selectedIndex.get() - 1);
        animateTransition(-1, prevIndex);
    }
    
    /**
     * Sets a callback to be invoked when the selection changes.
     * 
     * @param callback The callback receiving the newly selected item
     */
    public void setOnSelectionChanged(Consumer<String> callback) {
        this.onSelectionChanged = callback;
    }
    
    /**
     * Property for the selected index.
     */
    public IntegerProperty selectedIndexProperty() {
        return selectedIndex;
    }
    
    // ==================== CAROUSEL BUILDING ====================
    
    /**
     * Rebuilds the carousel UI with current items.
     */
    private void rebuildCarousel() {
        itemContainer.getChildren().clear();
        itemLabels.clear();
        
        if (items.isEmpty()) return;
        
        // Calculate how many items to display
        int displayCount = Math.min(items.size(), VISIBLE_SIDE_ITEMS * 2 + 1);
        
        // Create labels for visible items
        for (int i = 0; i < displayCount; i++) {
            Label label = createItemLabel(i);
            itemLabels.add(label);
            itemContainer.getChildren().add(label);
        }
        
        // Initial layout
        updateVisualEffects();
    }
    
    /**
     * Creates a styled label for a carousel item.
     */
    private Label createItemLabel(int index) {
        int itemIndex = normalizeIndex(selectedIndex.get() - VISIBLE_SIDE_ITEMS + index);
        String text = items.get(itemIndex);
        
        Label label = new Label(text);
        label.getStyleClass().add("carousel-item");
        label.setMinWidth(120);
        label.setAlignment(Pos.CENTER);
        
        // Click to select
        final int offset = index - VISIBLE_SIDE_ITEMS;
        label.setOnMouseClicked(e -> {
            if (!isAnimating && offset != 0) {
                if (offset > 0) {
                    for (int i = 0; i < Math.abs(offset); i++) {
                        next();
                    }
                } else {
                    for (int i = 0; i < Math.abs(offset); i++) {
                        previous();
                    }
                }
            }
        });
        
        return label;
    }
    
    // ==================== VISUAL EFFECTS ====================
    
    /**
     * Updates visual effects (scale, opacity, blur) based on current selection.
     */
    private void updateVisualEffects() {
        if (itemLabels.isEmpty()) return;
        
        int centerIndex = Math.min(VISIBLE_SIDE_ITEMS, itemLabels.size() / 2);
        
        for (int i = 0; i < itemLabels.size(); i++) {
            Label label = itemLabels.get(i);
            int distance = Math.abs(i - centerIndex);
            
            // Update item text
            int itemIndex = normalizeIndex(selectedIndex.get() - centerIndex + i);
            label.setText(items.get(itemIndex));
            
            if (distance == 0) {
                // Center item - highlighted
                applyHighlightedStyle(label);
            } else {
                // Side items - dimmed
                applyDimmedStyle(label, distance);
            }
        }
    }
    
    /**
     * Applies highlighted style to the center item.
     */
    private void applyHighlightedStyle(Label label) {
        label.setScaleX(CENTER_SCALE);
        label.setScaleY(CENTER_SCALE);
        label.setOpacity(CENTER_OPACITY);
        label.setEffect(null);
        label.getStyleClass().remove("carousel-item-dimmed");
        if (!label.getStyleClass().contains("carousel-item-selected")) {
            label.getStyleClass().add("carousel-item-selected");
        }
    }
    
    /**
     * Applies dimmed style to side items.
     */
    private void applyDimmedStyle(Label label, int distance) {
        double scale = SIDE_SCALE - (distance - 1) * 0.05;
        double opacity = SIDE_OPACITY - (distance - 1) * 0.15;
        double blur = SIDE_BLUR_RADIUS + (distance - 1) * 1.5;
        
        label.setScaleX(Math.max(0.6, scale));
        label.setScaleY(Math.max(0.6, scale));
        label.setOpacity(Math.max(0.2, opacity));
        label.setEffect(new GaussianBlur(blur));
        label.getStyleClass().remove("carousel-item-selected");
        if (!label.getStyleClass().contains("carousel-item-dimmed")) {
            label.getStyleClass().add("carousel-item-dimmed");
        }
    }
    
    // ==================== ANIMATION ====================
    
    /**
     * Animates the carousel transition in a direction.
     * 
     * @param direction 1 for next, -1 for previous
     * @param targetIndex The target index after animation
     */
    private void animateTransition(int direction, int targetIndex) {
        if (isAnimating) return;
        isAnimating = true;
        
        double translateAmount = (120 + ITEM_SPACING) * direction;
        
        Timeline timeline = new Timeline();
        
        // Animate all items
        for (Label label : itemLabels) {
            KeyValue kv = new KeyValue(
                label.translateXProperty(), 
                -translateAmount, 
                Interpolator.EASE_BOTH
            );
            timeline.getKeyFrames().add(new KeyFrame(
                Duration.millis(ANIMATION_DURATION_MS), 
                kv
            ));
        }
        
        timeline.setOnFinished(e -> {
            // Reset translations
            for (Label label : itemLabels) {
                label.setTranslateX(0);
            }
            
            // Update selection
            selectedIndex.set(targetIndex);
            isAnimating = false;
        });
        
        timeline.play();
    }
    
    /**
     * Animates directly to a specific index.
     */
    private void animateToIndex(int targetIndex) {
        int current = selectedIndex.get();
        int diff = targetIndex - current;
        
        // Handle wrap-around
        if (Math.abs(diff) > items.size() / 2) {
            if (diff > 0) {
                diff = diff - items.size();
            } else {
                diff = diff + items.size();
            }
        }
        
        if (diff > 0) {
            animateTransition(1, targetIndex);
        } else if (diff < 0) {
            animateTransition(-1, targetIndex);
        }
    }
    
    // ==================== NAVIGATION ====================
    
    /**
     * Sets up keyboard navigation.
     */
    private void setupKeyboardNavigation() {
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT) {
                previous();
                e.consume();
            } else if (e.getCode() == KeyCode.RIGHT) {
                next();
                e.consume();
            }
        });
    }
    
    /**
     * Sets up mouse scroll navigation.
     */
    private void setupMouseScrollNavigation() {
        addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.getDeltaX() > 0 || e.getDeltaY() > 0) {
                previous();
            } else if (e.getDeltaX() < 0 || e.getDeltaY() < 0) {
                next();
            }
            e.consume();
        });
    }
    
    // ==================== UTILITIES ====================
    
    /**
     * Normalizes an index to wrap around the items list.
     */
    private int normalizeIndex(int index) {
        if (items.isEmpty()) return 0;
        int size = items.size();
        return ((index % size) + size) % size;
    }
}

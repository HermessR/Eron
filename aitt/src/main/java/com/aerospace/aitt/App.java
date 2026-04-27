package com.aerospace.aitt;

import com.aerospace.aitt.shell.MainApp;

import javafx.application.Application;

/**
 * Application launcher class.
 * This class exists to work around JavaFX module loading issues when running
 * from a shaded/uber JAR. The main class should not extend Application directly.
 */
public class App {
    
    /**
     * Main entry point for the application.
     * Launches the JavaFX application via MainApp.
     */
    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}

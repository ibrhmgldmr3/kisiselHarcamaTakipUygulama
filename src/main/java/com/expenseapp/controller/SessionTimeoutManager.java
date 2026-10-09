package com.expenseapp.controller;

import com.expenseapp.config.DatabaseConfig;
import com.expenseapp.util.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.input.InputEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Oturum süresi sınırı. Giriş yapmış kullanıcı session.timeout.minutes (application.properties, varsayılan 15)
 * dakika boyunca hiçbir pencerede fare/klavye hareketi yapmazsa oturum kapatılır, açık pencereler (harcama/gelir
 * penceresi, onay kutuları) kapanır ve giriş ekranına dönülür.
 */
public final class SessionTimeoutManager {

    private static final Logger LOGGER = Logger.getLogger(SessionTimeoutManager.class.getName());
    private static final String TIMEOUT_KEY = "session.timeout.minutes";
    private static final long DEFAULT_TIMEOUT_MINUTES = 15;
    private static final Duration CHECK_INTERVAL = Duration.seconds(5);
    private static final String EXPIRED_MESSAGE = "Your session has expired. Please log in again.";
    /** Giriş/kayıt ekranlarının pencere boyutu. */
    private static final double LOGIN_WIDTH = 460;
    private static final double LOGIN_HEIGHT = 700;

    private static long lastActivity = System.nanoTime();

    private SessionTimeoutManager() {
    }

    public static void start(Stage mainStage) {
        long timeoutNanos = TimeUnit.MINUTES.toNanos(readTimeoutMinutes());
        EventHandler<InputEvent> onActivity = event -> lastActivity = System.nanoTime();

        // Sonradan açılan pencerelerdeki (harcama/gelir penceresi, onay kutusu) hareketler de süreyi sıfırlar.
        Window.getWindows().forEach(window -> window.addEventFilter(InputEvent.ANY, onActivity));
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(window -> window.addEventFilter(InputEvent.ANY, onActivity));
            }
        });

        Timeline timeline = new Timeline(new KeyFrame(CHECK_INTERVAL, event -> {
            if (SessionManager.getCurrentUser() != null && System.nanoTime() - lastActivity >= timeoutNanos) {
                expire(mainStage);
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /** Giriş ekranını ana pencereye yükler; message boş değilse giriş ekranında gösterilir. */
    static void showLoginScreen(Stage stage, String message) throws IOException {
        FXMLLoader loader = new FXMLLoader(SessionTimeoutManager.class.getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        stage.getScene().setRoot(root);
        stage.setWidth(LOGIN_WIDTH);
        stage.setHeight(LOGIN_HEIGHT);
        stage.centerOnScreen();
        if (message != null) {
            LoginController controller = loader.getController();
            controller.showMessage(message);
        }
    }

    private static void expire(Stage mainStage) {
        LOGGER.info("Oturum süresi doldu, kullanıcı çıkış yaptırıldı.");
        SessionManager.clear();
        for (Window window : new ArrayList<>(Window.getWindows())) {
            if (window != mainStage) {
                window.hide();
            }
        }
        try {
            showLoginScreen(mainStage, EXPIRED_MESSAGE);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Giriş ekranı yüklenemedi", e);
        }
    }

    private static long readTimeoutMinutes() {
        String value = DatabaseConfig.getProperty(TIMEOUT_KEY);
        if (value == null || value.isBlank()) {
            return DEFAULT_TIMEOUT_MINUTES;
        }
        try {
            long minutes = Long.parseLong(value.trim());
            if (minutes > 0) {
                return minutes;
            }
        } catch (NumberFormatException e) {
            // Aşağıda varsayılan değer kullanılır.
        }
        LOGGER.warning("Geçersiz " + TIMEOUT_KEY + " değeri: " + value + ". Varsayılan kullanılıyor.");
        return DEFAULT_TIMEOUT_MINUTES;
    }
}

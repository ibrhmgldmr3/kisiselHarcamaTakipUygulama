package com.expenseapp;

import com.expenseapp.config.DatabaseConfig;
import com.expenseapp.config.DatabaseInitializer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ExpenseTrackerApp extends Application {

    @Override
    public void start(Stage stage) {
        // Gün 1: yalnızca veritabanı bağlantısını doğrulayan geçici ekran.
        // Gün 2'de login.fxml ile değiştirilecek.
        Label status = new Label(checkDatabase());
        status.setWrapText(true);

        VBox root = new VBox(status);
        root.setPadding(new Insets(20));

        stage.setTitle("Kişisel Harcama Takip");
        stage.setScene(new Scene(root, 480, 160));
        stage.show();
    }

    private String checkDatabase() {
        try {
            DatabaseConfig.testConnection();
            DatabaseInitializer.initialize();
            String message = "PostgreSQL bağlantısı başarılı (SELECT 1). Tablolar hazır.";
            System.out.println(message);
            return message;
        } catch (Exception e) {
            String message = "Veritabanı bağlantısı başarısız: " + e.getMessage();
            System.err.println(message);
            return message;
        }
    }
}

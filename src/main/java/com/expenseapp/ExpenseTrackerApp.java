package com.expenseapp;

import com.expenseapp.config.DatabaseConfig;
import com.expenseapp.config.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ExpenseTrackerApp extends Application {

    @Override
    public void start(Stage stage) {
        Parent root;
        try {
            DatabaseConfig.testConnection();
            DatabaseInitializer.initialize();
            root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        } catch (Exception e) {
            String message = "Veritabanı bağlantısı başarısız: " + e.getMessage();
            System.err.println(message);
            Label status = new Label(message);
            status.setWrapText(true);
            VBox box = new VBox(status);
            box.setPadding(new Insets(20));
            root = box;
        }

        // Ekran geçişlerinde yalnızca root değiştiği için stil dosyası tüm ekranlarda geçerli kalır.
        Scene scene = new Scene(root, 440, 560);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("Kişisel Harcama Takip");
        stage.setScene(scene);
        stage.show();
    }
}

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

import java.util.logging.Level;
import java.util.logging.Logger;

public class ExpenseTrackerApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(ExpenseTrackerApp.class.getName());

    @Override
    public void start(Stage stage) {
        Parent root;
        try {
            DatabaseConfig.testConnection();
            DatabaseInitializer.initialize();
            root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Uygulama başlatılamadı", e);
            // IllegalStateException mesajları DatabaseConfig'in kendi açıklamalarıdır (ör. eksik application.properties);
            // PostgreSQL'den gelen ham hata mesajı kullanıcıya gösterilmez.
            String message = e instanceof IllegalStateException
                    ? e.getMessage()
                    : "Could not connect to the database. Please make sure PostgreSQL is running, "
                            + "check application.properties and restart the application.";
            Label status = new Label(message);
            status.setWrapText(true);
            VBox box = new VBox(status);
            box.setPadding(new Insets(20));
            root = box;
        }

        // Ekran geçişlerinde yalnızca root değiştiği için stil dosyası tüm ekranlarda geçerli kalır.
        Scene scene = new Scene(root, 440, 560);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("Personal Expense Tracker");
        stage.setScene(scene);
        stage.show();
    }
}

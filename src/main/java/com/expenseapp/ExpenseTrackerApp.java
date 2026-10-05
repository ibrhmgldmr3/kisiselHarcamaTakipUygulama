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
                    : "Veritabanına bağlanılamadı. PostgreSQL sunucusunun çalıştığını ve "
                            + "application.properties ayarlarını kontrol edip uygulamayı yeniden başlatın.";
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

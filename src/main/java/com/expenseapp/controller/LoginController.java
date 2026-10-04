package com.expenseapp.controller;

import com.expenseapp.model.User;
import com.expenseapp.service.AuthService;
import com.expenseapp.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        try {
            User user = authService.login(usernameField.getText(), passwordField.getText());
            SessionManager.setCurrentUser(user);
            // Ekran değişince usernameField sahneden ayrılacağı için pencere önceden alınır.
            Stage stage = (Stage) usernameField.getScene().getWindow();
            if (navigateTo("/fxml/dashboard.fxml")) {
                // Harcama tablosu için giriş ekranından daha geniş bir pencere gerekir.
                stage.setWidth(960);
                stage.setHeight(640);
                stage.centerOnScreen();
            }
        } catch (IllegalArgumentException e) {
            messageLabel.setText(e.getMessage());
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void goToRegister() {
        navigateTo("/fxml/register.fxml");
    }

    /** @return ekran yüklendiyse true */
    private boolean navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            usernameField.getScene().setRoot(root);
            return true;
        } catch (IOException e) {
            messageLabel.setText("Screen could not be loaded: " + fxmlPath);
            return false;
        }
    }
}

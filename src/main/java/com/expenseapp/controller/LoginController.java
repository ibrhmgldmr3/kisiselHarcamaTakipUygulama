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
            navigateTo("/fxml/dashboard.fxml");
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

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            usernameField.getScene().setRoot(root);
        } catch (IOException e) {
            messageLabel.setText("Screen could not be loaded: " + fxmlPath);
        }
    }
}

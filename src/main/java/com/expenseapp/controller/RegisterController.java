package com.expenseapp.controller;

import com.expenseapp.service.AuthService;
import com.expenseapp.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

public class RegisterController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        String error = ValidationUtil.validateRegistration(username, password, confirmPasswordField.getText());
        if (error != null) {
            showError(error);
            return;
        }

        try {
            authService.register(username, password);
            usernameField.clear();
            passwordField.clear();
            confirmPasswordField.clear();
            showSuccess("Account created. You can now log in.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            usernameField.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Login screen could not be loaded.");
        }
    }

    private void showError(String message) {
        messageLabel.getStyleClass().setAll("label", "error-label");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.getStyleClass().setAll("label", "success-label");
        messageLabel.setText(message);
    }
}

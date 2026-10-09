package com.expenseapp.controller;

import com.expenseapp.service.AuthService;
import com.expenseapp.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Şifremi unuttum ekranı. Önce kullanıcı adına ait güvenlik sorusu gösterilir; cevap doğruysa
 * yeni şifre kaydedilir. Kullanıcı adı ilk adımdan sonra kilitlenir, değiştirmek için ekrana yeniden girilir.
 */
public class ForgotPasswordController {

    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordController.class.getName());
    private static final String DATABASE_ERROR = "A database error occurred. Please try again later.";

    @FXML
    private TextField usernameField;
    @FXML
    private Button continueButton;
    @FXML
    private VBox resetBox;
    @FXML
    private Label questionLabel;
    @FXML
    private TextField securityAnswerField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        resetBox.managedProperty().bind(resetBox.visibleProperty());
        continueButton.managedProperty().bind(continueButton.visibleProperty());
    }

    @FXML
    private void handleContinue() {
        try {
            String question = authService.getSecurityQuestion(usernameField.getText());
            questionLabel.setText(question);
            usernameField.setEditable(false);
            continueButton.setVisible(false);
            resetBox.setVisible(true);
            messageLabel.setText("");
            securityAnswerField.requestFocus();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Güvenlik sorusu okunamadı", e);
            showError(DATABASE_ERROR);
        }
    }

    @FXML
    private void handleReset() {
        String newPassword = newPasswordField.getText();
        String error = ValidationUtil.validateSecurityAnswer(securityAnswerField.getText());
        if (error == null) {
            error = ValidationUtil.validatePasswordConfirmation(newPassword, confirmPasswordField.getText());
        }
        if (error != null) {
            showError(error);
            return;
        }

        try {
            authService.resetPassword(usernameField.getText(), securityAnswerField.getText(), newPassword);
            securityAnswerField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            resetBox.setDisable(true);
            showSuccess("Password updated. You can now log in.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Şifre sıfırlanırken veritabanı hatası", e);
            showError(DATABASE_ERROR);
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

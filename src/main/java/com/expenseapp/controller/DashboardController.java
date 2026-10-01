package com.expenseapp.controller;

import com.expenseapp.model.User;
import com.expenseapp.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Gün 2: yalnızca oturumdaki kullanıcıyı gösteren geçici ekran. */
public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private void initialize() {
        User user = SessionManager.getCurrentUser();
        welcomeLabel.setText("Welcome, " + user.getUsername() + " (id = " + user.getId() + ")");
    }
}

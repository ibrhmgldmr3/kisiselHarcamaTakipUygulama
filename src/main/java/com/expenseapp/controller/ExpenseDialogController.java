package com.expenseapp.controller;

import com.expenseapp.model.Category;
import com.expenseapp.model.Currency;
import com.expenseapp.model.Expense;
import com.expenseapp.service.ExpenseService;
import com.expenseapp.util.SessionManager;
import com.expenseapp.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Harcama ekleme ve düzenleme penceresi. Kaydetme başarılı olursa pencere kapanır. */
public class ExpenseDialogController {

    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField descriptionField;
    @FXML
    private ComboBox<Category> categoryComboBox;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<Currency> currencyComboBox;
    @FXML
    private Label messageLabel;

    private final ExpenseService expenseService = new ExpenseService();

    /** Düzenlenen harcamanın id'si; yeni harcamada null. */
    private Long expenseId;
    private boolean saved;

    @FXML
    private void initialize() {
        currencyComboBox.setItems(FXCollections.observableArrayList(Currency.values()));
        currencyComboBox.setValue(Currency.TRY);
        datePicker.setValue(LocalDate.now());

        try {
            long userId = SessionManager.getCurrentUser().getId();
            categoryComboBox.setItems(FXCollections.observableArrayList(expenseService.getUserCategories(userId)));
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
        }
    }

    /** @param expense düzenlenecek harcama; yeni harcama için null */
    public void setExpense(Expense expense) {
        if (expense == null) {
            return;
        }
        expenseId = expense.getId();
        datePicker.setValue(expense.getExpenseDate());
        descriptionField.setText(expense.getDescription());
        amountField.setText(expense.getAmount().toPlainString());
        currencyComboBox.setValue(expense.getCurrency());
        categoryComboBox.getItems().stream()
                .filter(c -> c.getId().equals(expense.getCategoryId()))
                .findFirst()
                .ifPresent(categoryComboBox::setValue);
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        Expense expense = new Expense();
        expense.setId(expenseId);
        try {
            expense.setExpenseDate(parseDate());
        } catch (DateTimeParseException e) {
            messageLabel.setText("Geçerli bir tarih girin.");
            return;
        }
        try {
            expense.setAmount(parseAmount());
        } catch (NumberFormatException e) {
            messageLabel.setText("Tutar geçerli bir sayı olmalı.");
            return;
        }
        expense.setDescription(descriptionField.getText());
        Category category = categoryComboBox.getValue();
        expense.setCategoryId(category == null ? null : category.getId());
        expense.setCurrency(currencyComboBox.getValue());

        try {
            long userId = SessionManager.getCurrentUser().getId();
            if (expenseId == null) {
                expenseService.createExpense(expense, userId);
            } else {
                expenseService.updateExpense(expense, userId);
            }
            saved = true;
            close();
        } catch (IllegalArgumentException e) {
            messageLabel.setText(e.getMessage());
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    /** Elle yazılıp henüz onaylanmamış tarihi de okur; boşsa null döner. */
    private LocalDate parseDate() {
        String text = datePicker.getEditor().getText();
        if (ValidationUtil.isBlank(text)) {
            return null;
        }
        LocalDate date = datePicker.getConverter().fromString(text.trim());
        datePicker.setValue(date);
        return date;
    }

    /** Ondalık ayırıcı olarak virgül de kabul edilir (ör. 12,50); boşsa null döner. */
    private BigDecimal parseAmount() {
        String text = amountField.getText();
        if (ValidationUtil.isBlank(text)) {
            return null;
        }
        return new BigDecimal(text.trim().replace(',', '.'));
    }

    private void close() {
        ((Stage) datePicker.getScene().getWindow()).close();
    }
}

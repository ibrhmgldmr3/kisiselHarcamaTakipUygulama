package com.expenseapp.controller;

import com.expenseapp.model.Category;
import com.expenseapp.model.Currency;
import com.expenseapp.model.Expense;
import com.expenseapp.service.ExpenseService;
import com.expenseapp.util.SessionManager;
import com.expenseapp.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Harcama ekleme ve düzenleme penceresi. Kaydetme (kur API çağrısı dahil) arka planda bir Task ile
 * yapılır, böylece API yavaşlasa da arayüz donmaz. Kaydetme başarılı olursa pencere kapanır.
 */
public class ExpenseDialogController {

    private static final String ADD_TITLE = "Add Expense";
    private static final String EDIT_TITLE = "Edit Expense";

    @FXML
    private Label titleLabel;
    @FXML
    private VBox formBox;
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
    private HBox loadingBox;
    @FXML
    private Label loadingLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private HBox buttonBox;

    private final ExpenseService expenseService = new ExpenseService();

    /** Düzenlenen harcamanın id'si; yeni harcamada null. */
    private Long expenseId;
    private boolean saved;
    private boolean saving;

    @FXML
    private void initialize() {
        loadingBox.managedProperty().bind(loadingBox.visibleProperty());
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
        titleLabel.setText(EDIT_TITLE);
        datePicker.setValue(expense.getExpenseDate());
        descriptionField.setText(expense.getDescription());
        amountField.setText(expense.getAmount().toPlainString());
        currencyComboBox.setValue(expense.getCurrency());
        categoryComboBox.getItems().stream()
                .filter(c -> c.getId().equals(expense.getCategoryId()))
                .findFirst()
                .ifPresent(categoryComboBox::setValue);
    }

    public String getTitle() {
        return expenseId == null ? ADD_TITLE : EDIT_TITLE;
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        if (saving) {
            return;
        }
        Expense expense = readForm();
        if (expense == null) {
            return;
        }

        long userId = SessionManager.getCurrentUser().getId();
        boolean isNew = expenseId == null;
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (isNew) {
                    expenseService.createExpense(expense, userId);
                } else {
                    expenseService.updateExpense(expense, userId);
                }
                return null;
            }
        };
        saveTask.setOnSucceeded(event -> {
            setLoading(false, null);
            saved = true;
            close();
        });
        saveTask.setOnFailed(event -> {
            setLoading(false, null);
            showError(toUserMessage(saveTask.getException()));
        });

        messageLabel.setText("");
        setLoading(true, expense.getCurrency() == Currency.TRY ? "Saving..." : "Fetching exchange rate...");
        Thread thread = new Thread(saveTask, "expense-save");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCancel() {
        if (!saving) {
            close();
        }
    }

    /** Formu doğrular; hata varsa mesajı gösterip null döner. */
    private Expense readForm() {
        Expense expense = new Expense();
        expense.setId(expenseId);

        LocalDate date;
        try {
            date = parseDate();
        } catch (DateTimeParseException e) {
            return fail("Please enter a valid date.");
        }
        if (date == null) {
            return fail("Date cannot be empty.");
        }
        expense.setExpenseDate(date);

        if (ValidationUtil.isBlank(descriptionField.getText())) {
            return fail("Description cannot be empty.");
        }
        expense.setDescription(descriptionField.getText());

        Category category = categoryComboBox.getValue();
        if (category == null) {
            return fail("Please select a category.");
        }
        expense.setCategoryId(category.getId());

        if (ValidationUtil.isBlank(amountField.getText())) {
            return fail("Amount cannot be empty.");
        }
        BigDecimal amount;
        try {
            amount = parseAmount();
        } catch (NumberFormatException e) {
            return fail("Amount must be a valid number.");
        }
        if (amount.signum() <= 0) {
            return fail("Amount must be greater than zero.");
        }
        expense.setAmount(amount);

        if (currencyComboBox.getValue() == null) {
            return fail("Please select a currency.");
        }
        expense.setCurrency(currencyComboBox.getValue());
        return expense;
    }

    private Expense fail(String message) {
        showError(message);
        return null;
    }

    /** Uzun mesajlar alt satıra geçebilsin diye pencere içeriğe göre yeniden boyutlandırılır. */
    private void showError(String message) {
        messageLabel.setText(message);
        getStage().sizeToScene();
    }

    /** Arka plandaki hatayı stack trace göstermeden anlaşılır bir mesaja çevirir. */
    private static String toUserMessage(Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof IOException) {
            // Doğrulama hatası veya "Döviz kuru alınamadı..." mesajı; kullanıcı tekrar deneyebilir.
            return error.getMessage();
        }
        if (error instanceof SQLException) {
            return "Database error: " + error.getMessage();
        }
        return "An unexpected error occurred. Please try again.";
    }

    /** Kaydetme sürerken formu kilitler ve yükleniyor durumunu gösterir. */
    private void setLoading(boolean loading, String text) {
        saving = loading;
        formBox.setDisable(loading);
        buttonBox.setDisable(loading);
        loadingLabel.setText(text);
        loadingBox.setVisible(loading);
        // Kayıt sürerken pencerenin X ile kapatılması engellenir; aksi halde kayıt tabloya yansımaz.
        getStage().setOnCloseRequest(loading ? Event::consume : null);
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

    /** Ondalık ayırıcı olarak virgül de kabul edilir (ör. 12,50). */
    private BigDecimal parseAmount() {
        return new BigDecimal(amountField.getText().trim().replace(',', '.'));
    }

    private Stage getStage() {
        return (Stage) datePicker.getScene().getWindow();
    }

    private void close() {
        getStage().close();
    }
}

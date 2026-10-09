package com.expenseapp.controller;

import com.expenseapp.model.Currency;
import com.expenseapp.model.Income;
import com.expenseapp.service.CurrencyApiException;
import com.expenseapp.service.IncomeService;
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

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gelir ekleme ve düzenleme penceresi. ExpenseDialogController ile aynı şekilde kaydetme (kur API çağrısı dahil)
 * arka planda bir Task ile yapılır; kaydetme başarılı olursa pencere kapanır.
 */
public class IncomeDialogController {

    private static final Logger LOGGER = Logger.getLogger(IncomeDialogController.class.getName());
    private static final String DATABASE_ERROR = "A database error occurred. Please try again later.";
    private static final String ADD_TITLE = "Add Income";
    private static final String EDIT_TITLE = "Edit Income";

    @FXML
    private Label titleLabel;
    @FXML
    private VBox formBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField descriptionField;
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

    private final IncomeService incomeService = new IncomeService();

    /** Düzenlenen gelirin id'si; yeni gelirde null. */
    private Long incomeId;
    private boolean saved;
    private boolean saving;

    @FXML
    private void initialize() {
        loadingBox.managedProperty().bind(loadingBox.visibleProperty());
        currencyComboBox.setItems(FXCollections.observableArrayList(Currency.values()));
        currencyComboBox.setValue(Currency.TRY);
        datePicker.setValue(LocalDate.now());
    }

    /** @param income düzenlenecek gelir; yeni gelir için null */
    public void setIncome(Income income) {
        if (income == null) {
            return;
        }
        incomeId = income.getId();
        titleLabel.setText(EDIT_TITLE);
        datePicker.setValue(income.getIncomeDate());
        descriptionField.setText(income.getDescription());
        amountField.setText(income.getAmount().toPlainString());
        currencyComboBox.setValue(income.getCurrency());
    }

    public String getTitle() {
        return incomeId == null ? ADD_TITLE : EDIT_TITLE;
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        if (saving) {
            return;
        }
        Income income = readForm();
        if (income == null) {
            return;
        }

        long userId = SessionManager.getCurrentUser().getId();
        boolean isNew = incomeId == null;
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (isNew) {
                    incomeService.createIncome(income, userId);
                } else {
                    incomeService.updateIncome(income, userId);
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
        setLoading(true, income.getCurrency() == Currency.TRY ? "Saving..." : "Fetching exchange rate...");
        Thread thread = new Thread(saveTask, "income-save");
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
    private Income readForm() {
        Income income = new Income();
        income.setId(incomeId);

        LocalDate date;
        try {
            date = parseDate();
        } catch (DateTimeParseException e) {
            return fail("Please enter a valid date.");
        }
        if (date == null) {
            return fail("Date cannot be empty.");
        }
        income.setIncomeDate(date);

        String error = ValidationUtil.validateDescription(descriptionField.getText());
        if (error != null) {
            return fail(error);
        }
        income.setDescription(descriptionField.getText());

        error = ValidationUtil.validateAmount(amountField.getText());
        if (error != null) {
            return fail(error);
        }
        income.setAmount(ValidationUtil.parseAmount(amountField.getText()));

        if (currencyComboBox.getValue() == null) {
            return fail("Please select a currency.");
        }
        income.setCurrency(currencyComboBox.getValue());
        return income;
    }

    private Income fail(String message) {
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
        if (error instanceof IllegalArgumentException) {
            return error.getMessage();
        }
        if (error instanceof CurrencyApiException) {
            LOGGER.log(Level.WARNING, "Döviz kuru alınamadı", error);
            return error.getMessage();
        }
        if (error instanceof SQLException) {
            LOGGER.log(Level.SEVERE, "Gelir kaydedilemedi", error);
            return DATABASE_ERROR;
        }
        LOGGER.log(Level.SEVERE, "Gelir kaydedilirken beklenmeyen hata", error);
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

    private Stage getStage() {
        return (Stage) datePicker.getScene().getWindow();
    }

    private void close() {
        getStage().close();
    }
}

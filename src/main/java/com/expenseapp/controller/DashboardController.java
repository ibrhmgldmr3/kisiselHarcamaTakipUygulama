package com.expenseapp.controller;

import com.expenseapp.model.Expense;
import com.expenseapp.model.User;
import com.expenseapp.service.ExpenseService;
import com.expenseapp.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

public class DashboardController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    /** TL tutarları ₺1,889.80 biçiminde gösterilir. */
    private static final DecimalFormat TRY_FORMAT = new DecimalFormat("₺#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    @FXML
    private Label welcomeLabel;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private TableView<Expense> expenseTable;
    @FXML
    private TableColumn<Expense, String> dateColumn;
    @FXML
    private TableColumn<Expense, String> descriptionColumn;
    @FXML
    private TableColumn<Expense, String> categoryColumn;
    @FXML
    private TableColumn<Expense, String> amountColumn;
    @FXML
    private TableColumn<Expense, String> currencyColumn;
    @FXML
    private TableColumn<Expense, String> amountTryColumn;
    @FXML
    private Label totalLabel;
    @FXML
    private Label messageLabel;

    private final ExpenseService expenseService = new ExpenseService();

    @FXML
    private void initialize() {
        User user = SessionManager.getCurrentUser();
        welcomeLabel.setText(user.getUsername());

        bindColumn(dateColumn, e -> e.getExpenseDate().format(DATE_FORMAT));
        bindColumn(descriptionColumn, Expense::getDescription);
        bindColumn(categoryColumn, Expense::getCategoryName);
        bindColumn(amountColumn, e -> formatAmount(e.getAmount()));
        bindColumn(currencyColumn, e -> e.getCurrency().name());
        bindColumn(amountTryColumn, e -> formatTry(e.getAmountTry()));

        // Satıra çift tıklama düzenleme penceresini açar.
        expenseTable.setRowFactory(table -> {
            TableRow<Expense> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openExpenseDialog(row.getItem());
                }
            });
            return row;
        });

        editButton.disableProperty().bind(expenseTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(expenseTable.getSelectionModel().selectedItemProperty().isNull());

        refreshExpenses();
    }

    /** Tabloyu ve Total Spending değerini veritabanından yeniden yükler; her ekleme/düzenleme/silmeden sonra çağrılır. */
    public void refreshExpenses() {
        try {
            long userId = SessionManager.getCurrentUser().getId();
            expenseTable.setItems(FXCollections.observableArrayList(expenseService.getUserExpenses(userId)));
            totalLabel.setText(formatTry(expenseService.getUserTotalTry(userId)));
            messageLabel.setText("");
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        openExpenseDialog(null);
    }

    @FXML
    private void handleEdit() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openExpenseDialog(selected);
        }
    }

    @FXML
    private void handleDelete() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType delete = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this expense?", cancel, delete);
        confirm.initOwner(expenseTable.getScene().getWindow());
        confirm.setTitle("Delete Expense");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(cancel) != delete) {
            return;
        }

        try {
            expenseService.deleteExpense(selected.getId(), SessionManager.getCurrentUser().getId());
            refreshExpenses();
        } catch (IllegalArgumentException e) {
            messageLabel.setText(e.getMessage());
            refreshExpenses();
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) expenseTable.getScene().getWindow();
            expenseTable.getScene().setRoot(root);
            stage.setWidth(460);
            stage.setHeight(600);
            stage.centerOnScreen();
        } catch (IOException e) {
            messageLabel.setText("Login screen could not be loaded.");
        }
    }

    /** @param expense düzenlenecek harcama; yeni harcama için null */
    private void openExpenseDialog(Expense expense) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/expense-dialog.fxml"));
            Parent root = loader.load();
            ExpenseDialogController controller = loader.getController();
            controller.setExpense(expense);

            Stage dialog = new Stage();
            dialog.initOwner(expenseTable.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle(controller.getTitle());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();

            if (controller.isSaved()) {
                refreshExpenses();
            }
        } catch (IOException e) {
            messageLabel.setText("Expense dialog could not be loaded.");
        }
    }

    private static void bindColumn(TableColumn<Expense, String> column, Function<Expense, String> getter) {
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
    }

    private static String formatAmount(BigDecimal amount) {
        return amount == null ? "" : amount.toPlainString();
    }

    private static String formatTry(BigDecimal amount) {
        return amount == null ? "" : TRY_FORMAT.format(amount);
    }
}

package com.expenseapp.controller;

import com.expenseapp.model.Expense;
import com.expenseapp.model.Income;
import com.expenseapp.model.User;
import com.expenseapp.service.ExpenseService;
import com.expenseapp.service.ExportService;
import com.expenseapp.service.IncomeService;
import com.expenseapp.util.FormatUtil;
import com.expenseapp.util.SessionManager;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardController {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String DATABASE_ERROR = "A database error occurred. Please try again later.";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    private Label welcomeLabel;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab incomeTab;
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
    private TableView<Income> incomeTable;
    @FXML
    private TableColumn<Income, String> incomeDateColumn;
    @FXML
    private TableColumn<Income, String> incomeDescriptionColumn;
    @FXML
    private TableColumn<Income, String> incomeAmountColumn;
    @FXML
    private TableColumn<Income, String> incomeCurrencyColumn;
    @FXML
    private TableColumn<Income, String> incomeAmountTryColumn;
    @FXML
    private Label totalIncomeLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label netLabel;
    @FXML
    private Label messageLabel;

    private final ExpenseService expenseService = new ExpenseService();
    private final IncomeService incomeService = new IncomeService();
    private final ExportService exportService = new ExportService();

    @FXML
    private void initialize() {
        User user = SessionManager.getCurrentUser();
        welcomeLabel.setText(user.getUsername());

        bindColumn(dateColumn, e -> e.getExpenseDate().format(DATE_FORMAT));
        bindColumn(descriptionColumn, Expense::getDescription);
        bindColumn(categoryColumn, Expense::getCategoryName);
        bindColumn(amountColumn, e -> formatAmount(e.getAmount()));
        bindColumn(currencyColumn, e -> e.getCurrency().name());
        bindColumn(amountTryColumn, e -> FormatUtil.formatTry(e.getAmountTry()));

        bindColumn(incomeDateColumn, i -> i.getIncomeDate().format(DATE_FORMAT));
        bindColumn(incomeDescriptionColumn, Income::getDescription);
        bindColumn(incomeAmountColumn, i -> formatAmount(i.getAmount()));
        bindColumn(incomeCurrencyColumn, i -> i.getCurrency().name());
        bindColumn(incomeAmountTryColumn, i -> FormatUtil.formatTry(i.getAmountTry()));

        // Satıra çift tıklama düzenleme penceresini açar.
        expenseTable.setRowFactory(table -> doubleClickRow(this::openExpenseDialog));
        incomeTable.setRowFactory(table -> doubleClickRow(this::openIncomeDialog));

        // Edit/Delete, açık olan sekmedeki tabloda seçili satır varsa etkindir.
        BooleanBinding noSelection = Bindings.createBooleanBinding(
                () -> isIncomeTabSelected()
                        ? incomeTable.getSelectionModel().getSelectedItem() == null
                        : expenseTable.getSelectionModel().getSelectedItem() == null,
                tabPane.getSelectionModel().selectedItemProperty(),
                expenseTable.getSelectionModel().selectedItemProperty(),
                incomeTable.getSelectionModel().selectedItemProperty());
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);

        refreshData();
    }

    /** Tabloları ve gelir/harcama/net toplamlarını veritabanından yeniden yükler; her ekleme/düzenleme/silmeden sonra çağrılır. */
    public void refreshData() {
        try {
            long userId = SessionManager.getCurrentUser().getId();
            expenseTable.setItems(FXCollections.observableArrayList(expenseService.getUserExpenses(userId)));
            incomeTable.setItems(FXCollections.observableArrayList(incomeService.getUserIncomes(userId)));

            BigDecimal totalIncome = incomeService.getUserTotalTry(userId);
            BigDecimal totalExpense = expenseService.getUserTotalTry(userId);
            BigDecimal net = totalIncome.subtract(totalExpense);
            totalIncomeLabel.setText(FormatUtil.formatTry(totalIncome));
            totalLabel.setText(FormatUtil.formatTry(totalExpense));
            netLabel.setText(FormatUtil.formatTry(net));
            netLabel.getStyleClass().setAll("label", "total-amount", net.signum() < 0 ? "net-negative" : "net-positive");
            messageLabel.setText("");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gelir/harcamalar yüklenemedi", e);
            showError(DATABASE_ERROR);
        }
    }

    @FXML
    private void handleAdd() {
        openExpenseDialog(null);
    }

    @FXML
    private void handleAddIncome() {
        openIncomeDialog(null);
    }

    @FXML
    private void handleEdit() {
        if (isIncomeTabSelected()) {
            Income selected = incomeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openIncomeDialog(selected);
            }
        } else {
            Expense selected = expenseTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openExpenseDialog(selected);
            }
        }
    }

    @FXML
    private void handleDelete() {
        boolean income = isIncomeTabSelected();
        Long selectedId = income
                ? idOf(incomeTable.getSelectionModel().getSelectedItem(), Income::getId)
                : idOf(expenseTable.getSelectionModel().getSelectedItem(), Expense::getId);
        if (selectedId == null) {
            return;
        }
        String recordName = income ? "income" : "expense";

        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType delete = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this " + recordName + "?", cancel, delete);
        confirm.initOwner(expenseTable.getScene().getWindow());
        confirm.setTitle(income ? "Delete Income" : "Delete Expense");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(cancel) != delete) {
            return;
        }

        try {
            long userId = SessionManager.getCurrentUser().getId();
            if (income) {
                incomeService.deleteIncome(selectedId, userId);
            } else {
                expenseService.deleteExpense(selectedId, userId);
            }
            refreshData();
        } catch (IllegalArgumentException e) {
            refreshData();
            showError(e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Kayıt silinemedi: " + recordName, e);
            showError(DATABASE_ERROR);
        }
    }

    /** Yalnızca giriş yapan kullanıcının harcamalarını seçilen CSV dosyasına yazar. */
    @FXML
    private void handleExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.setInitialFileName("expenses.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(expenseTable.getScene().getWindow());
        // Dosya penceresi açıkken oturum süresi dolmuş olabilir.
        if (file == null || SessionManager.getCurrentUser() == null) {
            return;
        }

        try {
            List<Expense> expenses = expenseService.getUserExpenses(SessionManager.getCurrentUser().getId());
            exportService.exportExpenses(expenses, file);
            showSuccess(expenses.size() + " expenses exported to " + file.getName() + ".");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CSV için harcamalar okunamadı", e);
            showError(DATABASE_ERROR);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "CSV dosyası yazılamadı: " + file, e);
            showError("CSV file could not be saved. Please check the file location and try again.");
        }
    }

    @FXML
    private void handleReports() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/reports.fxml"));
            expenseTable.getScene().setRoot(root);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Rapor ekranı yüklenemedi", e);
            showError("Reports screen could not be loaded.");
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();
        try {
            SessionTimeoutManager.showLoginScreen((Stage) expenseTable.getScene().getWindow(), null);
        } catch (IOException e) {
            showError("Login screen could not be loaded.");
        }
    }

    /** @param expense düzenlenecek harcama; yeni harcama için null */
    private void openExpenseDialog(Expense expense) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/expense-dialog.fxml"));
            Parent root = loader.load();
            ExpenseDialogController controller = loader.getController();
            controller.setExpense(expense);
            showDialog(root, controller.getTitle());
            if (controller.isSaved()) {
                refreshData();
            }
        } catch (IOException e) {
            showError("Expense dialog could not be loaded.");
        }
    }

    /** @param income düzenlenecek gelir; yeni gelir için null */
    private void openIncomeDialog(Income income) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/income-dialog.fxml"));
            Parent root = loader.load();
            IncomeDialogController controller = loader.getController();
            controller.setIncome(income);
            showDialog(root, controller.getTitle());
            if (controller.isSaved()) {
                refreshData();
            }
        } catch (IOException e) {
            showError("Income dialog could not be loaded.");
        }
    }

    /** Pencereyi dashboard'a bağlı modal olarak açar ve kapanana kadar bekler. */
    private void showDialog(Parent root, String title) {
        Stage dialog = new Stage();
        dialog.initOwner(expenseTable.getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(title);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private boolean isIncomeTabSelected() {
        return tabPane.getSelectionModel().getSelectedItem() == incomeTab;
    }

    private void showError(String message) {
        messageLabel.getStyleClass().setAll("label", "error-label");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.getStyleClass().setAll("label", "success-label");
        messageLabel.setText(message);
    }

    private static <T> TableRow<T> doubleClickRow(Consumer<T> onDoubleClick) {
        TableRow<T> row = new TableRow<>();
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) {
                onDoubleClick.accept(row.getItem());
            }
        });
        return row;
    }

    private static <T> Long idOf(T item, Function<T, Long> idGetter) {
        return item == null ? null : idGetter.apply(item);
    }

    private static <T> void bindColumn(TableColumn<T, String> column, Function<T, String> getter) {
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
    }

    private static String formatAmount(BigDecimal amount) {
        return amount == null ? "" : amount.toPlainString();
    }
}

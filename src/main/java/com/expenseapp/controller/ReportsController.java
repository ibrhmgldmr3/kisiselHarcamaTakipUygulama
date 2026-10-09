package com.expenseapp.controller;

import com.expenseapp.model.CategoryTotal;
import com.expenseapp.model.MonthlyTotal;
import com.expenseapp.service.ReportService;
import com.expenseapp.util.FormatUtil;
import com.expenseapp.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Seçilen tarih aralığı için gelir/harcama/net özeti, kategori bazında harcama dağılımı ve
 * aylık gelir-harcama grafiği. Varsayılan aralık yılın başından bugüne kadardır.
 */
public class ReportsController {

    private static final Logger LOGGER = Logger.getLogger(ReportsController.class.getName());
    private static final String DATABASE_ERROR = "A database error occurred. Please try again later.";
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private Label messageLabel;
    @FXML
    private Label incomeLabel;
    @FXML
    private Label expenseLabel;
    @FXML
    private Label netLabel;
    @FXML
    private TableView<CategoryTotal> categoryTable;
    @FXML
    private TableColumn<CategoryTotal, String> categoryNameColumn;
    @FXML
    private TableColumn<CategoryTotal, String> categoryAmountColumn;
    @FXML
    private TableColumn<CategoryTotal, String> categoryPercentColumn;
    @FXML
    private PieChart categoryChart;
    @FXML
    private BarChart<String, Number> monthlyChart;

    private final ReportService reportService = new ReportService();

    /** Kategori tablosundaki yüzdeler için seçili aralıktaki toplam harcama. */
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @FXML
    private void initialize() {
        LocalDate today = LocalDate.now();
        fromDatePicker.setValue(today.withDayOfYear(1));
        toDatePicker.setValue(today);
        // Mesaj yokken etiket yer kaplamasın.
        messageLabel.managedProperty().bind(messageLabel.textProperty().isNotEmpty());

        categoryNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().categoryName()));
        categoryAmountColumn.setCellValueFactory(
                data -> new SimpleStringProperty(FormatUtil.formatTry(data.getValue().totalTry())));
        categoryPercentColumn.setCellValueFactory(
                data -> new SimpleStringProperty(formatPercent(data.getValue().totalTry())));

        loadReport();
    }

    @FXML
    private void handleApply() {
        loadReport();
    }

    @FXML
    private void handleBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
            categoryTable.getScene().setRoot(root);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Dashboard yüklenemedi", e);
            messageLabel.setText("Dashboard could not be loaded.");
        }
    }

    private void loadReport() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        try {
            long userId = SessionManager.getCurrentUser().getId();
            List<CategoryTotal> categoryTotals = reportService.getExpensesByCategory(userId, from, to);
            List<MonthlyTotal> monthlyTotals = reportService.getMonthlyTotals(userId, from, to);

            BigDecimal totalIncome = BigDecimal.ZERO;
            totalExpense = BigDecimal.ZERO;
            for (MonthlyTotal month : monthlyTotals) {
                totalIncome = totalIncome.add(month.incomeTry());
                totalExpense = totalExpense.add(month.expenseTry());
            }
            BigDecimal net = totalIncome.subtract(totalExpense);
            incomeLabel.setText(FormatUtil.formatTry(totalIncome));
            expenseLabel.setText(FormatUtil.formatTry(totalExpense));
            netLabel.setText(FormatUtil.formatTry(net));
            netLabel.getStyleClass().setAll("label", "total-amount", net.signum() < 0 ? "net-negative" : "net-positive");

            categoryTable.setItems(FXCollections.observableArrayList(categoryTotals));
            categoryChart.setData(FXCollections.observableArrayList(categoryTotals.stream()
                    .map(c -> new PieChart.Data(c.categoryName(), c.totalTry().doubleValue()))
                    .toList()));
            showMonthlyChart(monthlyTotals);
            messageLabel.setText("");
        } catch (IllegalArgumentException e) {
            messageLabel.setText(e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Rapor yüklenemedi", e);
            messageLabel.setText(DATABASE_ERROR);
        }
    }

    private void showMonthlyChart(List<MonthlyTotal> monthlyTotals) {
        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");
        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Spending");
        for (MonthlyTotal month : monthlyTotals) {
            String label = month.month().format(MONTH_FORMAT);
            incomeSeries.getData().add(new XYChart.Data<>(label, month.incomeTry()));
            expenseSeries.getData().add(new XYChart.Data<>(label, month.expenseTry()));
        }
        monthlyChart.getData().setAll(List.of(incomeSeries, expenseSeries));
    }

    private String formatPercent(BigDecimal amount) {
        if (totalExpense.signum() == 0) {
            return "";
        }
        return amount.multiply(HUNDRED).divide(totalExpense, 1, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}

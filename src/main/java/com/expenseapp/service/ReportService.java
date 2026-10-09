package com.expenseapp.service;

import com.expenseapp.dao.ExpenseDAO;
import com.expenseapp.dao.IncomeDAO;
import com.expenseapp.model.CategoryTotal;
import com.expenseapp.model.MonthlyTotal;
import com.expenseapp.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Rapor ekranı için seçilen tarih aralığındaki gelir/harcama toplamları. Tüm tutarlar TL karşılıklarıdır
 * (amount_try); kuru alınamamış eski harcamalar dashboard toplamında olduğu gibi hesaba katılmaz.
 */
public class ReportService {

    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final IncomeDAO incomeDAO = new IncomeDAO();

    public List<CategoryTotal> getExpensesByCategory(long userId, LocalDate from, LocalDate to) throws SQLException {
        validateRange(from, to);
        return expenseDAO.getTotalsByCategory(userId, from, to);
    }

    /** Gelir veya harcaması olan ayları eski tarihten yeniye döndürür; olmayan taraf 0 kabul edilir. */
    public List<MonthlyTotal> getMonthlyTotals(long userId, LocalDate from, LocalDate to) throws SQLException {
        validateRange(from, to);
        Map<YearMonth, BigDecimal> incomes = incomeDAO.getMonthlyTotals(userId, from, to);
        Map<YearMonth, BigDecimal> expenses = expenseDAO.getMonthlyTotals(userId, from, to);

        TreeSet<YearMonth> months = new TreeSet<>(incomes.keySet());
        months.addAll(expenses.keySet());
        List<MonthlyTotal> totals = new ArrayList<>();
        for (YearMonth month : months) {
            totals.add(new MonthlyTotal(month,
                    incomes.getOrDefault(month, BigDecimal.ZERO),
                    expenses.getOrDefault(month, BigDecimal.ZERO)));
        }
        return totals;
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        String error = ValidationUtil.validateDateRange(from, to);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
    }
}

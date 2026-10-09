package com.expenseapp.service;

import com.expenseapp.dao.IncomeDAO;
import com.expenseapp.model.Income;
import com.expenseapp.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Gelir işlemleri. ExpenseService ile aynı kurallar geçerlidir: her metot userId alır, doğrulama hataları
 * IllegalArgumentException, veritabanı hataları SQLException, döviz kuru hataları CurrencyApiException olarak iletilir.
 */
public class IncomeService {

    private static final String INCOME_NOT_FOUND = "Income not found.";

    private final IncomeDAO incomeDAO = new IncomeDAO();
    private final CurrencyService currencyService = new CurrencyService();

    public List<Income> getUserIncomes(long userId) throws SQLException {
        return incomeDAO.findAllByUserId(userId);
    }

    /** Kullanıcının tüm gelirlerinin TL karşılıklarının (amount_try) toplamı. */
    public BigDecimal getUserTotalTry(long userId) throws SQLException {
        return incomeDAO.getTotalTryByUserId(userId);
    }

    public Income createIncome(Income income, long userId) throws SQLException, CurrencyApiException {
        income.setUserId(userId);
        validate(income);
        applyTryConversion(income);
        return incomeDAO.create(income);
    }

    public void updateIncome(Income income, long userId) throws SQLException, CurrencyApiException {
        income.setUserId(userId);
        if (income.getId() == null) {
            throw new IllegalArgumentException(INCOME_NOT_FOUND);
        }
        validate(income);
        applyTryConversion(income);
        if (!incomeDAO.update(income)) {
            throw new IllegalArgumentException(INCOME_NOT_FOUND);
        }
    }

    public void deleteIncome(long id, long userId) throws SQLException {
        if (!incomeDAO.delete(id, userId)) {
            throw new IllegalArgumentException(INCOME_NOT_FOUND);
        }
    }

    private void validate(Income income) {
        String error = ValidationUtil.validateIncome(income);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        income.setDescription(income.getDescription().trim());
    }

    /** Güncel kuru CurrencyService'ten alıp exchange_rate ve amount_try alanlarını doldurur. */
    private void applyTryConversion(Income income) throws CurrencyApiException {
        BigDecimal rate = currencyService.getExchangeRate(income.getCurrency());
        income.setExchangeRate(rate);
        income.setAmountTry(currencyService.convertToTry(income.getAmount(), rate));
    }
}

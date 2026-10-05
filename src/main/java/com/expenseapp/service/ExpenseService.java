package com.expenseapp.service;

import com.expenseapp.dao.CategoryDAO;
import com.expenseapp.dao.ExpenseDAO;
import com.expenseapp.model.Category;
import com.expenseapp.model.Expense;
import com.expenseapp.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Harcama ve kategori işlemleri. Her metot userId alır ve işlemi o kullanıcıyla sınırlar.
 * Doğrulama hataları, kullanıcıya gösterilecek mesajla birlikte IllegalArgumentException
 * olarak fırlatılır; veritabanı hataları SQLException, döviz kuru hataları CurrencyApiException olarak iletilir.
 */
public class ExpenseService {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Gıda", "Ulaşım", "Eğlence", "Fatura", "Alışveriş", "Sağlık", "Eğitim", "Diğer");
    private static final String EXPENSE_NOT_FOUND = "Expense not found.";

    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final CurrencyService currencyService = new CurrencyService();

    public List<Expense> getUserExpenses(long userId) throws SQLException {
        return expenseDAO.findAllByUserId(userId);
    }

    /** Kullanıcının tüm harcamalarının TL karşılıklarının (amount_try) toplamı. */
    public BigDecimal getUserTotalTry(long userId) throws SQLException {
        return expenseDAO.getTotalTryByUserId(userId);
    }

    /** Kullanıcının kategorilerini döndürür; hiç kategorisi yoksa önce varsayılanları oluşturur. */
    public List<Category> getUserCategories(long userId) throws SQLException {
        List<Category> categories = categoryDAO.findAllByUserId(userId);
        if (!categories.isEmpty()) {
            return categories;
        }
        for (String name : DEFAULT_CATEGORIES) {
            categoryDAO.create(new Category(userId, name));
        }
        return categoryDAO.findAllByUserId(userId);
    }

    public Expense createExpense(Expense expense, long userId) throws SQLException, CurrencyApiException {
        expense.setUserId(userId);
        validate(expense);
        applyTryConversion(expense);
        return expenseDAO.create(expense);
    }

    public void updateExpense(Expense expense, long userId) throws SQLException, CurrencyApiException {
        expense.setUserId(userId);
        if (expense.getId() == null) {
            throw new IllegalArgumentException(EXPENSE_NOT_FOUND);
        }
        validate(expense);
        applyTryConversion(expense);
        if (!expenseDAO.update(expense)) {
            throw new IllegalArgumentException(EXPENSE_NOT_FOUND);
        }
    }

    public void deleteExpense(long id, long userId) throws SQLException {
        if (!expenseDAO.delete(id, userId)) {
            throw new IllegalArgumentException(EXPENSE_NOT_FOUND);
        }
    }

    private void validate(Expense expense) throws SQLException {
        String error = ValidationUtil.validateExpense(expense);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        expense.setDescription(expense.getDescription().trim());
        // Başka bir kullanıcının kategori id'si gönderilirse kategori seçilmemiş gibi reddedilir.
        if (categoryDAO.findById(expense.getCategoryId(), expense.getUserId()).isEmpty()) {
            throw new IllegalArgumentException("Please select a category.");
        }
    }

    /**
     * Güncel kuru CurrencyService'ten alıp exchange_rate ve amount_try alanlarını doldurur.
     * Ekleme ve düzenlemede her seferinde yeniden hesaplanır.
     */
    private void applyTryConversion(Expense expense) throws CurrencyApiException {
        BigDecimal rate = currencyService.getExchangeRate(expense.getCurrency());
        expense.setExchangeRate(rate);
        expense.setAmountTry(currencyService.convertToTry(expense.getAmount(), rate));
    }
}

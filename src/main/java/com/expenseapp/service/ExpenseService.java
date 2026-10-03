package com.expenseapp.service;

import com.expenseapp.dao.CategoryDAO;
import com.expenseapp.dao.ExpenseDAO;
import com.expenseapp.model.Category;
import com.expenseapp.model.Expense;
import com.expenseapp.util.ValidationUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Harcama ve kategori işlemleri. Her metot userId alır ve işlemi o kullanıcıyla sınırlar.
 * Doğrulama hataları, kullanıcıya gösterilecek mesajla birlikte IllegalArgumentException
 * olarak fırlatılır; veritabanı hataları SQLException, döviz kuru hataları IOException olarak iletilir.
 */
public class ExpenseService {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Gıda", "Ulaşım", "Eğlence", "Fatura", "Alışveriş", "Sağlık", "Eğitim", "Diğer");
    private static final int MAX_DESCRIPTION_LENGTH = 255;
    /** expenses.amount NUMERIC(12, 2) sütununa sığabilecek en büyük değerin üst sınırı. */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000000");
    private static final String EXPENSE_NOT_FOUND = "Harcama bulunamadı.";

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

    public Expense createExpense(Expense expense, long userId) throws SQLException, IOException {
        expense.setUserId(userId);
        validate(expense);
        applyTryConversion(expense);
        return expenseDAO.create(expense);
    }

    public void updateExpense(Expense expense, long userId) throws SQLException, IOException {
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
        if (expense.getExpenseDate() == null) {
            throw new IllegalArgumentException("Geçerli bir tarih girin.");
        }
        if (ValidationUtil.isBlank(expense.getDescription())) {
            throw new IllegalArgumentException("Açıklama boş olamaz.");
        }
        expense.setDescription(expense.getDescription().trim());
        if (expense.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Açıklama en fazla " + MAX_DESCRIPTION_LENGTH + " karakter olabilir.");
        }
        if (expense.getCategoryId() == null
                || categoryDAO.findById(expense.getCategoryId(), expense.getUserId()).isEmpty()) {
            throw new IllegalArgumentException("Geçerli bir kategori seçin.");
        }
        BigDecimal amount = expense.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Tutar 0'dan büyük olmalı.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Tutar en fazla 2 ondalık basamak içerebilir.");
        }
        if (amount.compareTo(MAX_AMOUNT) >= 0) {
            throw new IllegalArgumentException("Tutar çok büyük.");
        }
        if (expense.getCurrency() == null) {
            throw new IllegalArgumentException("Para birimi seçin.");
        }
    }

    /**
     * Güncel kuru CurrencyService'ten alıp exchange_rate ve amount_try alanlarını doldurur.
     * Ekleme ve düzenlemede her seferinde yeniden hesaplanır.
     */
    private void applyTryConversion(Expense expense) throws IOException {
        BigDecimal rate = currencyService.getExchangeRate(expense.getCurrency());
        expense.setExchangeRate(rate);
        expense.setAmountTry(currencyService.convertToTry(expense.getAmount(), rate));
    }
}

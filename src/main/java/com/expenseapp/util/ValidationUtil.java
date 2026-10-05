package com.expenseapp.util;

import com.expenseapp.model.Expense;

import java.math.BigDecimal;

/** Form ve servis doğrulamaları. Hata varsa kullanıcıya gösterilecek mesajı, yoksa null döndürür. */
public final class ValidationUtil {

    /** users.username VARCHAR(50) ile aynı sınır. */
    private static final int MAX_USERNAME_LENGTH = 50;
    /** expenses.description VARCHAR(255) ile aynı sınır. */
    private static final int MAX_DESCRIPTION_LENGTH = 255;
    /** expenses.amount NUMERIC(12, 2) sütununa sığabilecek en büyük değerin üst sınırı. */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000000");

    private ValidationUtil() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String validateUsername(String username) {
        if (isBlank(username)) {
            return "Username cannot be empty.";
        }
        if (username.trim().length() > MAX_USERNAME_LENGTH) {
            return "Username cannot exceed " + MAX_USERNAME_LENGTH + " characters.";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (isBlank(password)) {
            return "Password cannot be empty.";
        }
        return null;
    }

    public static String validateLogin(String username, String password) {
        if (isBlank(username)) {
            return "Username cannot be empty.";
        }
        return validatePassword(password);
    }

    public static String validateRegistration(String username, String password, String confirmPassword) {
        String error = validateUsername(username);
        if (error == null) {
            error = validatePassword(password);
        }
        if (error != null) {
            return error;
        }
        if (isBlank(confirmPassword)) {
            return "Confirm password cannot be empty.";
        }
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }
        return null;
    }

    public static String validateDescription(String description) {
        if (isBlank(description)) {
            return "Description cannot be empty.";
        }
        if (description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            return "Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters.";
        }
        return null;
    }

    /** Formdaki tutar metnini doğrular. */
    public static String validateAmount(String text) {
        if (isBlank(text)) {
            return "Amount is required.";
        }
        try {
            return validateAmount(parseAmount(text));
        } catch (NumberFormatException e) {
            return "Amount must be a valid number.";
        }
    }

    public static String validateAmount(BigDecimal amount) {
        if (amount == null) {
            return "Amount is required.";
        }
        if (amount.signum() <= 0) {
            return "Amount must be greater than zero.";
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            return "Amount can have at most 2 decimal places.";
        }
        if (amount.compareTo(MAX_AMOUNT) >= 0) {
            return "Amount is too large.";
        }
        return null;
    }

    /** Ondalık ayırıcı olarak virgül de kabul edilir (ör. 12,50). Geçersizse NumberFormatException fırlatır. */
    public static BigDecimal parseAmount(String text) {
        return new BigDecimal(text.trim().replace(',', '.'));
    }

    /** Harcama alanlarını doğrular. Kategorinin kullanıcıya ait olduğu ExpenseService'te ayrıca kontrol edilir. */
    public static String validateExpense(Expense expense) {
        if (expense.getExpenseDate() == null) {
            return "Please enter a valid date.";
        }
        String error = validateDescription(expense.getDescription());
        if (error != null) {
            return error;
        }
        if (expense.getCategoryId() == null) {
            return "Please select a category.";
        }
        error = validateAmount(expense.getAmount());
        if (error != null) {
            return error;
        }
        if (expense.getCurrency() == null) {
            return "Please select a currency.";
        }
        return null;
    }
}

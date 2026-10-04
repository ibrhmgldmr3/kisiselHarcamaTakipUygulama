package com.expenseapp.util;

/** Form doğrulamaları. Hata varsa kullanıcıya gösterilecek mesajı, yoksa null döndürür. */
public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String validateLogin(String username, String password) {
        if (isBlank(username)) {
            return "Username cannot be empty.";
        }
        if (isBlank(password)) {
            return "Password cannot be empty.";
        }
        return null;
    }

    public static String validateRegistration(String username, String password, String confirmPassword) {
        String error = validateLogin(username, password);
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
}

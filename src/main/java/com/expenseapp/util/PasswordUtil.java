package com.expenseapp.util;

import org.mindrot.jbcrypt.BCrypt;

/** Şifreleri BCrypt ile hash'ler ve doğrular. Veritabanına yalnızca hash yazılır. */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String plainPassword, String storedHash) {
        return BCrypt.checkpw(plainPassword, storedHash);
    }
}

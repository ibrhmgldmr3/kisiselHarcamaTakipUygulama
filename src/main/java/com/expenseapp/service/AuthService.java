package com.expenseapp.service;

import com.expenseapp.dao.UserDAO;
import com.expenseapp.model.User;
import com.expenseapp.util.PasswordUtil;
import com.expenseapp.util.ValidationUtil;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Kayıt ve giriş mantığı. Doğrulama hataları, kullanıcıya gösterilecek mesajla birlikte
 * IllegalArgumentException olarak fırlatılır; veritabanı hataları SQLException olarak iletilir.
 */
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password.";
    private static final String USERNAME_EXISTS = "Username already exists.";
    /** PostgreSQL unique_violation hata kodu. */
    private static final String UNIQUE_VIOLATION = "23505";

    private final UserDAO userDAO = new UserDAO();

    public User register(String username, String password) throws SQLException {
        String error = ValidationUtil.validateUsername(username);
        if (error == null) {
            error = ValidationUtil.validatePassword(password);
        }
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        String trimmedUsername = username.trim();
        if (userDAO.existsByUsername(trimmedUsername)) {
            throw new IllegalArgumentException(USERNAME_EXISTS);
        }

        User user = new User(trimmedUsername, PasswordUtil.hashPassword(password));
        try {
            return userDAO.createUser(user);
        } catch (SQLException e) {
            // Kontrol ile INSERT arasında aynı kullanıcı adı başka biri tarafından alınmış olabilir.
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new IllegalArgumentException(USERNAME_EXISTS);
            }
            throw e;
        }
    }

    public User login(String username, String password) throws SQLException {
        String error = ValidationUtil.validateLogin(username, password);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        Optional<User> user = userDAO.findByUsername(username.trim());
        // Kullanıcı yok ve şifre yanlış durumlarında aynı mesaj verilir.
        if (user.isEmpty() || !PasswordUtil.verifyPassword(password, user.get().getPasswordHash())) {
            throw new IllegalArgumentException(INVALID_CREDENTIALS);
        }
        return user.get();
    }
}

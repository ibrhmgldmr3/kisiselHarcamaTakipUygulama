package com.expenseapp.service;

import com.expenseapp.dao.UserDAO;
import com.expenseapp.model.User;
import com.expenseapp.util.PasswordUtil;
import com.expenseapp.util.ValidationUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Kayıt, giriş ve şifre sıfırlama mantığı. Doğrulama hataları, kullanıcıya gösterilecek mesajla birlikte
 * IllegalArgumentException olarak fırlatılır; veritabanı hataları SQLException olarak iletilir.
 */
public class AuthService {

    /** Kayıt ekranında seçilebilen güvenlik soruları; şifremi unuttum ekranında cevabı sorulur. */
    public static final List<String> SECURITY_QUESTIONS = List.of(
            "What was the name of your first pet?",
            "In which city were you born?",
            "What was the name of your primary school?",
            "What is your mother's maiden name?");

    private static final String INVALID_CREDENTIALS = "Invalid username or password.";
    private static final String USERNAME_EXISTS = "Username already exists.";
    private static final String RESET_UNAVAILABLE = "No account with a security question was found for this username.";
    private static final String WRONG_SECURITY_ANSWER = "Security answer is incorrect.";
    /** PostgreSQL unique_violation hata kodu. */
    private static final String UNIQUE_VIOLATION = "23505";

    private final UserDAO userDAO = new UserDAO();

    public User register(String username, String password, String securityQuestion, String securityAnswer)
            throws SQLException {
        String error = ValidationUtil.validateUsername(username);
        if (error == null) {
            error = ValidationUtil.validatePassword(password);
        }
        // List.of(...).contains(null) NullPointerException fırlattığı için null ayrıca kontrol edilir.
        if (error == null && (securityQuestion == null || !SECURITY_QUESTIONS.contains(securityQuestion))) {
            error = "Please select a security question.";
        }
        if (error == null) {
            error = ValidationUtil.validateSecurityAnswer(securityAnswer);
        }
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        String trimmedUsername = username.trim();
        if (userDAO.existsByUsername(trimmedUsername)) {
            throw new IllegalArgumentException(USERNAME_EXISTS);
        }

        User user = new User(trimmedUsername, PasswordUtil.hashPassword(password));
        user.setSecurityQuestion(securityQuestion);
        user.setSecurityAnswerHash(PasswordUtil.hashPassword(normalizeAnswer(securityAnswer)));
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

    /** Şifremi unuttum ekranının ilk adımı: kullanıcının güvenlik sorusunu döndürür. */
    public String getSecurityQuestion(String username) throws SQLException {
        return findResettableUser(username).getSecurityQuestion();
    }

    /** Güvenlik sorusunun cevabı doğruysa şifreyi yenisiyle değiştirir. */
    public void resetPassword(String username, String securityAnswer, String newPassword) throws SQLException {
        String error = ValidationUtil.validateSecurityAnswer(securityAnswer);
        if (error == null) {
            error = ValidationUtil.validatePassword(newPassword);
        }
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        User user = findResettableUser(username);
        if (!PasswordUtil.verifyPassword(normalizeAnswer(securityAnswer), user.getSecurityAnswerHash())) {
            throw new IllegalArgumentException(WRONG_SECURITY_ANSWER);
        }
        if (!userDAO.updatePassword(user.getId(), PasswordUtil.hashPassword(newPassword))) {
            throw new IllegalArgumentException(RESET_UNAVAILABLE);
        }
    }

    /** Kullanıcı yoksa veya güvenlik sorusu tanımlı değilse (eski hesaplar) aynı mesaj verilir. */
    private User findResettableUser(String username) throws SQLException {
        if (ValidationUtil.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        Optional<User> user = userDAO.findByUsername(username.trim());
        if (user.isEmpty() || user.get().getSecurityAnswerHash() == null) {
            throw new IllegalArgumentException(RESET_UNAVAILABLE);
        }
        return user.get();
    }

    /** Cevap büyük/küçük harf ve baştaki/sondaki boşluklardan bağımsız karşılaştırılır. */
    private static String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase(Locale.ROOT);
    }
}

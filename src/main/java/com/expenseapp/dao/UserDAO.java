package com.expenseapp.dao;

import com.expenseapp.config.DatabaseConfig;
import com.expenseapp.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserDAO {

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = """
                SELECT id, username, password_hash, security_question, security_answer_hash, created_at
                FROM users WHERE username = ?
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Kullanıcıyı ekler; oluşan id ve created_at değerlerini nesneye yazar. */
    public User createUser(User user) throws SQLException {
        String sql = """
                INSERT INTO users (username, password_hash, security_question, security_answer_hash)
                VALUES (?, ?, ?, ?)
                RETURNING id, created_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getSecurityQuestion());
            ps.setString(4, user.getSecurityAnswerHash());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                user.setId(rs.getLong("id"));
                user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                return user;
            }
        }
    }

    /** @return kayıt güncellendiyse true */
    public boolean updatePassword(long userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSecurityQuestion(rs.getString("security_question"));
        user.setSecurityAnswerHash(rs.getString("security_answer_hash"));
        user.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return user;
    }
}

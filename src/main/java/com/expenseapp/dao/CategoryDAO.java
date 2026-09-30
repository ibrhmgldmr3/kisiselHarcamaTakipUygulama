package com.expenseapp.dao;

import com.expenseapp.config.DatabaseConfig;
import com.expenseapp.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Tüm sorgular user_id ile sınırlandırılır; kullanıcı başkasının kategorisine erişemez. */
public class CategoryDAO {

    public List<Category> findAllByUserId(long userId) throws SQLException {
        String sql = "SELECT id, user_id, name FROM categories WHERE user_id = ? ORDER BY name";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Category> categories = new ArrayList<>();
                while (rs.next()) {
                    categories.add(mapRow(rs));
                }
                return categories;
            }
        }
    }

    public Optional<Category> findByIdAndUserId(long id, long userId) throws SQLException {
        String sql = "SELECT id, user_id, name FROM categories WHERE id = ? AND user_id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Kategoriyi ekler; oluşan id değerini nesneye yazar. */
    public Category insert(Category category) throws SQLException {
        String sql = "INSERT INTO categories (user_id, name) VALUES (?, ?) RETURNING id";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, category.getUserId());
            ps.setString(2, category.getName());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                category.setId(rs.getLong("id"));
                return category;
            }
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setUserId(rs.getLong("user_id"));
        category.setName(rs.getString("name"));
        return category;
    }
}

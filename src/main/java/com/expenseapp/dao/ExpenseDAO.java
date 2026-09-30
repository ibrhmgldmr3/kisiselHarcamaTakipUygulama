package com.expenseapp.dao;

import com.expenseapp.config.DatabaseConfig;
import com.expenseapp.model.Currency;
import com.expenseapp.model.Expense;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tüm sorgular user_id ile sınırlandırılır. Güncelleme ve silmede de "id = ? AND user_id = ?"
 * kullanıldığı için başka bir kullanıcının harcama id'sini bilmek yeterli olmaz.
 */
public class ExpenseDAO {

    private static final String SELECT_COLUMNS = """
            SELECT e.id, e.user_id, e.category_id, c.name AS category_name, e.expense_date,
                   e.description, e.amount, e.currency, e.exchange_rate, e.amount_try,
                   e.created_at, e.updated_at
            FROM expenses e
            JOIN categories c ON c.id = e.category_id
            """;

    public List<Expense> findAllByUserId(long userId) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE e.user_id = ? ORDER BY e.expense_date DESC, e.id DESC";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Expense> expenses = new ArrayList<>();
                while (rs.next()) {
                    expenses.add(mapRow(rs));
                }
                return expenses;
            }
        }
    }

    public Optional<Expense> findByIdAndUserId(long id, long userId) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE e.id = ? AND e.user_id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Harcamayı ekler; oluşan id ve zaman damgalarını nesneye yazar. */
    public Expense insert(Expense expense) throws SQLException {
        String sql = """
                INSERT INTO expenses (user_id, category_id, expense_date, description,
                                      amount, currency, exchange_rate, amount_try)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id, created_at, updated_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, expense.getUserId());
            ps.setLong(2, expense.getCategoryId());
            ps.setObject(3, expense.getExpenseDate());
            ps.setString(4, expense.getDescription());
            ps.setBigDecimal(5, expense.getAmount());
            ps.setString(6, expense.getCurrency().name());
            ps.setBigDecimal(7, expense.getExchangeRate());
            ps.setBigDecimal(8, expense.getAmountTry());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                expense.setId(rs.getLong("id"));
                expense.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                expense.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
                return expense;
            }
        }
    }

    /** @return kayıt güncellendiyse true; harcama yoksa veya başka kullanıcıya aitse false */
    public boolean update(Expense expense) throws SQLException {
        String sql = """
                UPDATE expenses
                SET category_id = ?, expense_date = ?, description = ?, amount = ?,
                    currency = ?, exchange_rate = ?, amount_try = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, expense.getCategoryId());
            ps.setObject(2, expense.getExpenseDate());
            ps.setString(3, expense.getDescription());
            ps.setBigDecimal(4, expense.getAmount());
            ps.setString(5, expense.getCurrency().name());
            ps.setBigDecimal(6, expense.getExchangeRate());
            ps.setBigDecimal(7, expense.getAmountTry());
            ps.setLong(8, expense.getId());
            ps.setLong(9, expense.getUserId());
            return ps.executeUpdate() == 1;
        }
    }

    /** @return kayıt silindiyse true; harcama yoksa veya başka kullanıcıya aitse false */
    public boolean delete(long id, long userId) throws SQLException {
        String sql = "DELETE FROM expenses WHERE id = ? AND user_id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    public BigDecimal getTotalTryByUserId(long userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount_try), 0) FROM expenses WHERE user_id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    private Expense mapRow(ResultSet rs) throws SQLException {
        Expense expense = new Expense();
        expense.setId(rs.getLong("id"));
        expense.setUserId(rs.getLong("user_id"));
        expense.setCategoryId(rs.getLong("category_id"));
        expense.setCategoryName(rs.getString("category_name"));
        expense.setExpenseDate(rs.getObject("expense_date", LocalDate.class));
        expense.setDescription(rs.getString("description"));
        expense.setAmount(rs.getBigDecimal("amount"));
        expense.setCurrency(Currency.valueOf(rs.getString("currency")));
        expense.setExchangeRate(rs.getBigDecimal("exchange_rate"));
        expense.setAmountTry(rs.getBigDecimal("amount_try"));
        expense.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        expense.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return expense;
    }
}

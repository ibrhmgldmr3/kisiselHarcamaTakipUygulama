package com.expenseapp.dao;

import com.expenseapp.config.DatabaseConfig;
import com.expenseapp.model.Currency;
import com.expenseapp.model.Income;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** ExpenseDAO ile aynı şekilde tüm sorgular user_id ile sınırlandırılır. */
public class IncomeDAO {

    private static final String SELECT_COLUMNS = """
            SELECT id, user_id, income_date, description, amount, currency, exchange_rate, amount_try,
                   created_at, updated_at
            FROM incomes
            """;

    public List<Income> findAllByUserId(long userId) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE user_id = ? ORDER BY income_date DESC, id DESC";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Income> incomes = new ArrayList<>();
                while (rs.next()) {
                    incomes.add(mapRow(rs));
                }
                return incomes;
            }
        }
    }

    /** Geliri ekler; oluşan id ve zaman damgalarını nesneye yazar. */
    public Income create(Income income) throws SQLException {
        String sql = """
                INSERT INTO incomes (user_id, income_date, description, amount, currency, exchange_rate, amount_try)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id, created_at, updated_at
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, income.getUserId());
            ps.setObject(2, income.getIncomeDate());
            ps.setString(3, income.getDescription());
            ps.setBigDecimal(4, income.getAmount());
            ps.setString(5, income.getCurrency().name());
            ps.setBigDecimal(6, income.getExchangeRate());
            ps.setBigDecimal(7, income.getAmountTry());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                income.setId(rs.getLong("id"));
                income.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                income.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
                return income;
            }
        }
    }

    /** @return kayıt güncellendiyse true; gelir yoksa veya başka kullanıcıya aitse false */
    public boolean update(Income income) throws SQLException {
        String sql = """
                UPDATE incomes
                SET income_date = ?, description = ?, amount = ?, currency = ?, exchange_rate = ?,
                    amount_try = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, income.getIncomeDate());
            ps.setString(2, income.getDescription());
            ps.setBigDecimal(3, income.getAmount());
            ps.setString(4, income.getCurrency().name());
            ps.setBigDecimal(5, income.getExchangeRate());
            ps.setBigDecimal(6, income.getAmountTry());
            ps.setLong(7, income.getId());
            ps.setLong(8, income.getUserId());
            return ps.executeUpdate() == 1;
        }
    }

    /** @return kayıt silindiyse true; gelir yoksa veya başka kullanıcıya aitse false */
    public boolean delete(long id, long userId) throws SQLException {
        String sql = "DELETE FROM incomes WHERE id = ? AND user_id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    public BigDecimal getTotalTryByUserId(long userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount_try), 0) FROM incomes WHERE user_id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    /** Tarih aralığındaki (sınırlar dahil) gelirlerin ay bazında TL toplamları. */
    public Map<YearMonth, BigDecimal> getMonthlyTotals(long userId, LocalDate from, LocalDate to) throws SQLException {
        String sql = """
                SELECT CAST(date_trunc('month', income_date) AS DATE) AS month, SUM(amount_try) AS total
                FROM incomes
                WHERE user_id = ? AND income_date BETWEEN ? AND ?
                GROUP BY month
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setObject(2, from);
            ps.setObject(3, to);
            try (ResultSet rs = ps.executeQuery()) {
                Map<YearMonth, BigDecimal> totals = new TreeMap<>();
                while (rs.next()) {
                    totals.put(YearMonth.from(rs.getObject("month", LocalDate.class)), rs.getBigDecimal("total"));
                }
                return totals;
            }
        }
    }

    private Income mapRow(ResultSet rs) throws SQLException {
        Income income = new Income();
        income.setId(rs.getLong("id"));
        income.setUserId(rs.getLong("user_id"));
        income.setIncomeDate(rs.getObject("income_date", LocalDate.class));
        income.setDescription(rs.getString("description"));
        income.setAmount(rs.getBigDecimal("amount"));
        income.setCurrency(Currency.valueOf(rs.getString("currency")));
        income.setExchangeRate(rs.getBigDecimal("exchange_rate"));
        income.setAmountTry(rs.getBigDecimal("amount_try"));
        income.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        income.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
        return income;
    }
}

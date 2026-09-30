package com.expenseapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Veritabanı bağlantı bilgilerini classpath'teki config/application.properties dosyasından okur
 * ve JDBC bağlantısı üretir. Bu dosya repoya eklenmez; şablonu application.properties.example'dır.
 */
public final class DatabaseConfig {

    private static final String CONFIG_PATH = "/config/application.properties";

    private static Properties properties;

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        Properties props = getProperties();
        return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.username"),
                props.getProperty("db.password"));
    }

    /** Bağlantıyı "SELECT 1" ile doğrular; başarısızsa SQLException fırlatır. */
    public static void testConnection() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT 1")) {
            if (!rs.next() || rs.getInt(1) != 1) {
                throw new SQLException("SELECT 1 beklenen sonucu döndürmedi.");
            }
        }
    }

    private static synchronized Properties getProperties() {
        if (properties == null) {
            properties = loadProperties();
        }
        return properties;
    }

    private static Properties loadProperties() {
        try (InputStream in = DatabaseConfig.class.getResourceAsStream(CONFIG_PATH)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Yapılandırma dosyası bulunamadı: src/main/resources" + CONFIG_PATH
                                + ". application.properties.example dosyasını kopyalayıp düzenleyin.");
            }
            Properties props = new Properties();
            props.load(in);
            for (String key : new String[]{"db.url", "db.username", "db.password"}) {
                if (props.getProperty(key) == null || props.getProperty(key).isBlank()) {
                    throw new IllegalStateException("Yapılandırmada '" + key + "' değeri eksik.");
                }
            }
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Yapılandırma dosyası okunamadı: " + CONFIG_PATH, e);
        }
    }
}

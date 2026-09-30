package com.expenseapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Uygulama açılışında db/schema.sql betiğini çalıştırarak tabloları oluşturur. */
public final class DatabaseInitializer {

    private static final String SCHEMA_PATH = "/db/schema.sql";

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        String schemaSql = readSchema();
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(schemaSql);
        }
    }

    private static String readSchema() {
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(SCHEMA_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Şema dosyası bulunamadı: " + SCHEMA_PATH);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Şema dosyası okunamadı: " + SCHEMA_PATH, e);
        }
    }
}

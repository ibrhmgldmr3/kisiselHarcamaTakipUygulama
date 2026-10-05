package com.expenseapp.service;

import com.expenseapp.model.Expense;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Verilen harcama listesini CSV dosyasına yazar. Veritabanına erişmez; listeyi çağıran taraf
 * ExpenseService üzerinden yalnızca giriş yapan kullanıcının harcamalarıyla alır.
 */
public class ExportService {

    private static final String HEADER = "Date,Description,Category,Amount,Currency,TRY";
    private static final String LINE_END = "\r\n";

    public void exportExpenses(List<Expense> expenses, File file) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            // BOM, Excel'in UTF-8'i tanıyıp Türkçe karakterleri (Gıda, Ulaşım) doğru göstermesini sağlar.
            writer.write('﻿');
            writer.write(HEADER + LINE_END);
            for (Expense expense : expenses) {
                writer.write(String.join(",",
                        expense.getExpenseDate().toString(),
                        escape(expense.getDescription()),
                        escape(expense.getCategoryName()),
                        expense.getAmount().toPlainString(),
                        expense.getCurrency().name(),
                        formatTry(expense.getAmountTry())));
                writer.write(LINE_END);
            }
        }
    }

    /** Virgül, çift tırnak veya satır sonu içeren alan tırnak içine alınır; içindeki tırnaklar ikilenir. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Kuru alınamamış eski kayıtlarda TL karşılığı boş bırakılır. */
    private static String formatTry(BigDecimal amountTry) {
        return amountTry == null ? "" : amountTry.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

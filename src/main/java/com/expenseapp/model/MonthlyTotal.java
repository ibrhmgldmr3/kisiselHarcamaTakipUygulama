package com.expenseapp.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Rapor ekranı: bir aydaki gelir ve harcamaların TL toplamları. */
public record MonthlyTotal(YearMonth month, BigDecimal incomeTry, BigDecimal expenseTry) {
}

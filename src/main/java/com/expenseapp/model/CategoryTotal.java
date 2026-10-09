package com.expenseapp.model;

import java.math.BigDecimal;

/** Rapor ekranı: bir kategorideki harcamaların seçilen tarih aralığındaki TL toplamı. */
public record CategoryTotal(String categoryName, BigDecimal totalTry) {
}

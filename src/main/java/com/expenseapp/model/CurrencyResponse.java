package com.expenseapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Currency API (Frankfurter) yanıtı. Örnek:
 * {"amount":1.0,"base":"USD","date":"2026-10-02","rates":{"TRY":49.145}}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyResponse {

    private BigDecimal amount;
    private String base;
    private String date;
    private Map<String, BigDecimal> rates;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Map<String, BigDecimal> getRates() {
        return rates;
    }

    public void setRates(Map<String, BigDecimal> rates) {
        this.rates = rates;
    }
}

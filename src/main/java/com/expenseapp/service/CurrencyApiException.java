package com.expenseapp.service;

/** Döviz kuru alınamadığında fırlatılır. Mesajı doğrudan kullanıcıya gösterilebilir; asıl hata cause içindedir. */
public class CurrencyApiException extends Exception {

    public CurrencyApiException(String message) {
        super(message);
    }

    public CurrencyApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

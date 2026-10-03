package com.expenseapp.service;

import com.expenseapp.config.DatabaseConfig;
import com.expenseapp.model.Currency;
import com.expenseapp.model.CurrencyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Döviz kurunu REST API'den (application.properties içindeki currency.api.url) alır ve TL karşılığını
 * hesaplar. Bağlantı hatası, zaman aşımı, HTTP hata kodu, bozuk JSON veya eksik TRY kuru durumlarında
 * kullanıcıya gösterilecek mesajla birlikte IOException fırlatılır.
 */
public class CurrencyService {

    private static final String API_URL_KEY = "currency.api.url";
    private static final String RATE_UNAVAILABLE =
            "Döviz kuru alınamadı. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    /** expenses.exchange_rate NUMERIC(12, 6) ile aynı ölçek. */
    private static final int RATE_SCALE = 6;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 1 birim para biriminin TL karşılığı. TRY için API çağrılmadan 1 döner. */
    public BigDecimal getExchangeRate(Currency currency) throws IOException {
        if (currency == Currency.TRY) {
            return BigDecimal.ONE;
        }
        String apiUrl = DatabaseConfig.getProperty(API_URL_KEY);
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IOException("Yapılandırmada '" + API_URL_KEY + "' değeri eksik.");
        }
        try {
            return fetchRate(apiUrl.trim(), currency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(RATE_UNAVAILABLE, e);
        } catch (IOException | IllegalArgumentException e) {
            throw new IOException(RATE_UNAVAILABLE, e);
        }
    }

    public BigDecimal convertToTry(BigDecimal amount, BigDecimal exchangeRate) {
        return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal fetchRate(String apiUrl, Currency currency) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + "?base=" + currency.name() + "&symbols=TRY"))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Currency API HTTP " + response.statusCode() + " döndürdü.");
        }

        CurrencyResponse body = objectMapper.readValue(response.body(), CurrencyResponse.class);
        if (body == null || !currency.name().equals(body.getBase()) || body.getRates() == null) {
            throw new IOException("Currency API yanıtı beklenen formatta değil.");
        }
        BigDecimal rate = body.getRates().get(Currency.TRY.name());
        if (rate == null || rate.signum() <= 0) {
            throw new IOException("Currency API yanıtında geçerli TRY kuru yok.");
        }
        return rate.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }
}

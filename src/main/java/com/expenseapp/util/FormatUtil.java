package com.expenseapp.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Dashboard ve rapor ekranında ortak kullanılan tutar biçimlendirmesi. */
public final class FormatUtil {

    /** TL tutarları ₺1,889.80 biçiminde gösterilir. */
    private static final DecimalFormat TRY_FORMAT = new DecimalFormat("₺#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private FormatUtil() {
    }

    public static String formatTry(BigDecimal amount) {
        return amount == null ? "" : TRY_FORMAT.format(amount);
    }
}

package com.rad.center.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FormatUtil {
    private static final DecimalFormat VND_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        VND_FORMAT = new DecimalFormat("#,###", symbols);
    }

    public static String formatVnd(long amount) {
        return VND_FORMAT.format(amount) + " VNĐ";
    }

    public static String formatNumber(long amount) {
        return VND_FORMAT.format(amount);
    }

    public static String parseCleanDigits(String input) {
        if (input == null) return "0";
        return input.replaceAll("[^0-9-]", "");
    }
}


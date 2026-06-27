package com.business.managementsystem.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts a non-negative BigDecimal AED amount to its English words representation.
 *
 * Examples:
 *   1497710.00  → "One Million Four Hundred Ninety Seven Thousand Seven Hundred Ten
 *                   United Arab Emirates Dirham Only"
 *   2479080.15  → "Two Million Four Hundred Seventy Nine Thousand Eighty
 *                   United Arab Emirates Dirham And Fifteen Fils Only"
 *   0.00        → "Zero United Arab Emirates Dirham Only"
 *
 * Used exclusively for Tax Invoice PDF generation.
 */
public final class AedWords {

    private static final String[] ONES = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private AedWords() {}

    /**
     * Convert a non-negative BigDecimal AED amount to words.
     * Negative values are treated as zero.
     * The fils part is rounded to 2 decimal places.
     *
     * @param amount the AED amount
     * @return the words representation
     */
    public static String convert(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
            amount = BigDecimal.ZERO;

        amount = amount.setScale(2, RoundingMode.HALF_UP);

        long dirhams = amount.longValue();
        int  fils    = amount.remainder(BigDecimal.ONE)
                             .multiply(BigDecimal.valueOf(100))
                             .setScale(0, RoundingMode.HALF_UP)
                             .intValue();

        StringBuilder sb = new StringBuilder();

        if (dirhams == 0) {
            sb.append("Zero");
        } else {
            sb.append(toLongWords(dirhams));
        }

        sb.append(" United Arab Emirates Dirham");

        if (fils > 0) {
            sb.append(" And ").append(toLongWords(fils)).append(" Fils");
        }

        sb.append(" Only");
        return sb.toString();
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private static String toLongWords(long n) {
        if (n == 0)    return "";
        if (n < 20)    return ONES[(int) n];
        if (n < 100) {
            String t   = TENS[(int) (n / 10)];
            int    rem = (int) (n % 10);
            return rem == 0 ? t : t + " " + ONES[rem];
        }
        if (n < 1_000L) {
            String h   = ONES[(int) (n / 100)] + " Hundred";
            long   rem = n % 100;
            return rem == 0 ? h : h + " " + toLongWords(rem);
        }
        if (n < 1_000_000L) {
            String k   = toLongWords(n / 1_000L) + " Thousand";
            long   rem = n % 1_000L;
            return rem == 0 ? k : k + " " + toLongWords(rem);
        }
        if (n < 1_000_000_000L) {
            String m   = toLongWords(n / 1_000_000L) + " Million";
            long   rem = n % 1_000_000L;
            return rem == 0 ? m : m + " " + toLongWords(rem);
        }
        // Billions — unlikely for invoice amounts but handled for safety
        String b   = toLongWords(n / 1_000_000_000L) + " Billion";
        long   rem = n % 1_000_000_000L;
        return rem == 0 ? b : b + " " + toLongWords(rem);
    }
}

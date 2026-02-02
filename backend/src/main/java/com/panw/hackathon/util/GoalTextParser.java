package com.panw.hackathon.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic parser for plain-English goals like:
 * - "Save $5,000 by 2026-06-15"
 * - "Put aside 3k in 6 months"
 * - "$1200 for a trip within 180 days"
 * - "$2.5k by December 2026"
 */
public class GoalTextParser {

    public static class ParsedGoal {
        public final BigDecimal targetAmount;
        public final int monthsToDeadline;
        public final LocalDate deadlineDate;
        public final String note;
        public ParsedGoal(BigDecimal targetAmount, int monthsToDeadline, LocalDate deadlineDate, String note) {
            this.targetAmount = targetAmount;
            this.monthsToDeadline = monthsToDeadline;
            this.deadlineDate = deadlineDate;
            this.note = note;
        }
    }

      // Match $ amounts without truncating (avoids matching $300 in $3000)
      // Strategy: prefer long ungrouped digits first, or comma-grouped; ensure no trailing digit directly after
      private static final Pattern AMOUNT_DOLLAR = Pattern.compile(
          "\\$\\s*((?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]{1,2})?)(?![0-9])"
      );
    private static final Pattern AMOUNT_NUMBER_DOLLARS = Pattern.compile("([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:usd|dollars|bucks)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMOUNT_WITH_SUFFIX = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*([kmbKMB])");

    // Patterns for relative deadlines
    private static final Pattern DEADLINE_IN = Pattern.compile("(?:in|within)\\s+([0-9]{1,4})\\s*(day|days|month|months|year|years)", Pattern.CASE_INSENSITIVE);

    // Patterns for absolute deadlines
    private static final Pattern DEADLINE_DATE_ISO = Pattern.compile("by\\s+([0-9]{4}-[0-9]{2}-[0-9]{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEADLINE_DATE_SLASH = Pattern.compile("by\\s+([0-9]{1,2}/[0-9]{1,2}/[0-9]{2,4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEADLINE_MONTH_NAME = Pattern.compile("by\\s+(January|February|March|April|May|June|July|August|September|October|November|December)\\s*(?:([0-9]{1,2})\\s*,?\\s*)?([0-9]{4})?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEADLINE_END_OF_MONTH = Pattern.compile("by\\s+end\\s+of\\s+(January|February|March|April|May|June|July|August|September|October|November|December)\\s*([0-9]{4})?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEADLINE_NEXT_MONTH = Pattern.compile("by\\s+next\\s+(January|February|March|April|May|June|July|August|September|October|November|December)", Pattern.CASE_INSENSITIVE);

    public static ParsedGoal parse(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.isEmpty()) return null;

        BigDecimal amount = parseAmount(s);
        Integer months = null;
        LocalDate deadline = parseAbsoluteDeadline(s);

        if (deadline == null) {
            months = parseRelativeMonths(s);
            if (months != null) {
                deadline = LocalDate.now().plusMonths(months).withDayOfMonth(YearMonth.from(LocalDate.now().plusMonths(months)).lengthOfMonth());
            }
        } else {
            months = monthsBetween(LocalDate.now(), deadline);
        }

        if (amount == null || months == null || months <= 0) {
            return null;
        }
        return new ParsedGoal(amount.setScale(2, RoundingMode.HALF_UP), months, deadline, null);
    }

    private static BigDecimal parseAmount(String s) {
        Matcher m1 = AMOUNT_DOLLAR.matcher(s);
        if (m1.find()) {
            String raw = m1.group(1).replace(",", "");
            try { return new BigDecimal(raw); } catch (NumberFormatException ignored) {}
        }
        Matcher m2 = AMOUNT_NUMBER_DOLLARS.matcher(s);
        if (m2.find()) {
            String raw = m2.group(1);
            try { return new BigDecimal(raw); } catch (NumberFormatException ignored) {}
        }
        Matcher m3 = AMOUNT_WITH_SUFFIX.matcher(s);
        if (m3.find()) {
            String num = m3.group(1);
            String suf = m3.group(2);
            try {
                BigDecimal base = new BigDecimal(num);
                switch (suf.toLowerCase(Locale.ROOT)) {
                    case "k": return base.multiply(BigDecimal.valueOf(1_000));
                    case "m": return base.multiply(BigDecimal.valueOf(1_000_000));
                    case "b": return base.multiply(BigDecimal.valueOf(1_000_000_000));
                }
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static LocalDate parseAbsoluteDeadline(String s) {
        Matcher iso = DEADLINE_DATE_ISO.matcher(s);
        if (iso.find()) {
            try { return LocalDate.parse(iso.group(1)); } catch (DateTimeParseException ignored) {}
        }
        Matcher slash = DEADLINE_DATE_SLASH.matcher(s);
        if (slash.find()) {
            String raw = slash.group(1);
            DateTimeFormatter fmt = raw.length() == 10 ? DateTimeFormatter.ofPattern("M/d/yyyy") : DateTimeFormatter.ofPattern("M/d/yy");
            try { return LocalDate.parse(raw, fmt); } catch (DateTimeParseException ignored) {}
        }
        Matcher monthName = DEADLINE_MONTH_NAME.matcher(s);
        if (monthName.find()) {
            String m = monthName.group(1);
            String dayStr = monthName.group(2);
            String yearStr = monthName.group(3);
            Month month = Month.valueOf(m.toUpperCase(Locale.ROOT));
            int year = (yearStr != null) ? Integer.parseInt(yearStr) : LocalDate.now().getYear();
            int day = (dayStr != null) ? Integer.parseInt(dayStr) : YearMonth.of(year, month).lengthOfMonth();
            return LocalDate.of(year, month, day);
        }
        Matcher endOf = DEADLINE_END_OF_MONTH.matcher(s);
        if (endOf.find()) {
            String m = endOf.group(1);
            String yearStr = endOf.group(2);
            Month month = Month.valueOf(m.toUpperCase(Locale.ROOT));
            int year = (yearStr != null) ? Integer.parseInt(yearStr) : LocalDate.now().getYear();
            YearMonth ym = YearMonth.of(year, month);
            return LocalDate.of(year, month, ym.lengthOfMonth());
        }
        Matcher nextMonth = DEADLINE_NEXT_MONTH.matcher(s);
        if (nextMonth.find()) {
            String m = nextMonth.group(1);
            Month month = Month.valueOf(m.toUpperCase(Locale.ROOT));
            int year = LocalDate.now().getYear();
            if (month.getValue() <= LocalDate.now().getMonthValue()) {
                year += 1;
            }
            YearMonth ym = YearMonth.of(year, month);
            return LocalDate.of(year, month, ym.lengthOfMonth());
        }
        return null;
    }

    private static Integer parseRelativeMonths(String s) {
        Matcher m = DEADLINE_IN.matcher(s);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            String unit = m.group(2).toLowerCase(Locale.ROOT);
            switch (unit) {
                case "day": case "days":
                    return Math.max(1, (int) Math.ceil(n / 30.0));
                case "month": case "months":
                    return Math.max(1, n);
                case "year": case "years":
                    return Math.max(1, n * 12);
            }
        }
        return null;
    }

    private static int monthsBetween(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) return 0;
        YearMonth s = YearMonth.from(start);
        YearMonth e = YearMonth.from(end);
        int months = (e.getYear() - s.getYear()) * 12 + (e.getMonthValue() - s.getMonthValue());
        if (end.getDayOfMonth() >= start.getDayOfMonth()) months += 1;
        return Math.max(1, months);
    }
}

package com.panw.hackathon.util;

import com.panw.hackathon.model.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CsvParser {

    public static List<Transaction> parseTransactions(MultipartFile file) throws IOException {
        List<Transaction> list = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // default
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, format)) {
            for (CSVRecord record : parser) {
                String dateStr = get(record, "date");
                LocalDate date = parseDate(dateStr, df);
                double amount = Double.parseDouble(get(record, "amount"));
                String merchant = get(record, "merchant");
                String category = get(record, "category");
                String account = get(record, "account");

                list.add(new Transaction(date, amount, merchant, category, account));
            }
        }
        return list;
    }

    private static String get(CSVRecord record, String key) {
        String v = record.isMapped(key) ? record.get(key) : null;
        return v == null ? "" : v;
    }

    private static LocalDate parseDate(String val, DateTimeFormatter defaultFmt) {
        String s = val == null ? "" : val.trim();
        if (s.isEmpty()) return LocalDate.now();
        String[] patterns = new String[]{"yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy"};
        for (String p : patterns) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {}
        }
        try {
            return LocalDate.parse(s, defaultFmt);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}

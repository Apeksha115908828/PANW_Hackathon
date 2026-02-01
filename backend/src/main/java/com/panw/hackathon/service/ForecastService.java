package com.panw.hackathon.service;

import com.panw.hackathon.model.ForecastResult;
import com.panw.hackathon.model.GoalRequest;
import com.panw.hackathon.model.Suggestion;
import com.panw.hackathon.model.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ForecastService {
    private static final Set<String> FIXED_CATEGORIES = new HashSet<>(Arrays.asList(
            "Rent", "Mortgage", "Loan", "Utilities", "Internet", "Phone", "Insurance", "Tuition", "Subscriptions"
    ));
    private static final Set<String> DISCRETIONARY_CATEGORIES = new HashSet<>(Arrays.asList(
            "Dining", "Restaurants", "Shopping", "Rideshare", "Entertainment", "Travel", "Hobbies"
    ));

    public ForecastResult analyze(List<Transaction> txns, GoalRequest goal) {
        int baselineMonths = 3; // v1 assumption
        Map<YearMonth, List<Transaction>> byMonth = groupByMonth(txns);
        List<YearMonth> months = byMonth.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .limit(baselineMonths)
                .sorted()
                .collect(Collectors.toList());

        List<Double> monthlyIncome = new ArrayList<>();
        List<Double> monthlyFixed = new ArrayList<>();
        List<Double> monthlyVariable = new ArrayList<>();

        Map<String, List<Double>> categorySpendPerMonth = new HashMap<>();

        for (YearMonth ym : months) {
            List<Transaction> mtx = byMonth.getOrDefault(ym, Collections.emptyList());
            double income = mtx.stream().filter(t -> t.getAmount() > 0).mapToDouble(Transaction::getAmount).sum();
            double fixed = mtx.stream().filter(t -> t.getAmount() < 0 && FIXED_CATEGORIES.contains(normalize(t.getCategory())))
                    .mapToDouble(t -> -t.getAmount()).sum();
            double variable = mtx.stream().filter(t -> t.getAmount() < 0 && !FIXED_CATEGORIES.contains(normalize(t.getCategory())))
                    .mapToDouble(t -> -t.getAmount()).sum();

            monthlyIncome.add(income);
            monthlyFixed.add(fixed);
            monthlyVariable.add(variable);

            // track per-category variable baselines
            Map<String, Double> varByCat = mtx.stream()
                    .filter(t -> t.getAmount() < 0)
                    .collect(Collectors.groupingBy(
                            t -> normalize(t.getCategory()),
                            Collectors.summingDouble(t -> -t.getAmount())
                    ));
            for (Map.Entry<String, Double> e : varByCat.entrySet()) {
                categorySpendPerMonth.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue());
            }
        }

        List<Double> capacity = new ArrayList<>();
        for (int i = 0; i < months.size(); i++) {
            double cap = monthlyIncome.get(i) - monthlyFixed.get(i) - monthlyVariable.get(i);
            capacity.add(cap);
        }

        double p10 = percentile(capacity, 10);
        double p50 = percentile(capacity, 50);
        double p90 = percentile(capacity, 90);

        BigDecimal requiredMonthly = goal.getTargetAmount()
                .subtract(goal.getCurrentSavings() == null ? BigDecimal.ZERO : goal.getCurrentSavings())
                .divide(BigDecimal.valueOf(goal.getMonthsToDeadline()), 2, RoundingMode.HALF_UP);

        BigDecimal buffer = goal.getBuffer() == null ? BigDecimal.ZERO : goal.getBuffer();
        BigDecimal projectedMonthlyToGoal = BigDecimal.valueOf(Math.max(0, p50) ).subtract(buffer).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        String status;
        boolean onTrack;
        if (BigDecimal.valueOf(p50).compareTo(requiredMonthly) >= 0) {
            status = "on_track";
            onTrack = true;
        } else if (BigDecimal.valueOf(p90).compareTo(requiredMonthly) >= 0) {
            status = "borderline";
            onTrack = false;
        } else {
            status = "off_track";
            onTrack = false;
        }

        BigDecimal forecastedBalanceAtDeadlineP50 = (BigDecimal.valueOf(p50).subtract(buffer).max(BigDecimal.ZERO))
                .multiply(BigDecimal.valueOf(goal.getMonthsToDeadline()))
                .add(goal.getCurrentSavings() == null ? BigDecimal.ZERO : goal.getCurrentSavings())
                .setScale(2, RoundingMode.HALF_UP);

        List<Suggestion> suggestions = new ArrayList<>();
        BigDecimal gap = requiredMonthly.subtract(projectedMonthlyToGoal);
        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.addAll(generateVariableTrimSuggestions(categorySpendPerMonth, goal, gap));
            suggestions.add(generateTimelineLeverSuggestion(goal));
            suggestions.add(generateIncomeLeverSuggestion());
        }

        ForecastResult result = new ForecastResult();
        result.setStatus(status);
        result.setOnTrack(onTrack);
        result.setRequiredMonthly(requiredMonthly);
        result.setP10(BigDecimal.valueOf(p10).setScale(2, RoundingMode.HALF_UP));
        result.setP50(BigDecimal.valueOf(p50).setScale(2, RoundingMode.HALF_UP));
        result.setP90(BigDecimal.valueOf(p90).setScale(2, RoundingMode.HALF_UP));
        result.setProjectedMonthlyToGoal(projectedMonthlyToGoal);
        result.setForecastedBalanceAtDeadlineP50(forecastedBalanceAtDeadlineP50);
        result.setSuggestions(suggestions);
        return result;
    }

    private Map<YearMonth, List<Transaction>> groupByMonth(List<Transaction> txns) {
        return txns.stream().collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate())));
    }

    private double percentile(List<Double> values, int pct) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        double pos = (pct / 100.0) * (sorted.size() - 1);
        int idx = (int) Math.floor(pos);
        int idx2 = Math.min(sorted.size() - 1, idx + 1);
        double frac = pos - idx;
        return sorted.get(idx) * (1 - frac) + sorted.get(idx2) * frac;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim();
    }

    private List<Suggestion> generateVariableTrimSuggestions(Map<String, List<Double>> perCat, GoalRequest goal, BigDecimal gap) {
        // Rank discretionary categories by median spend
        List<Map.Entry<String, Double>> medians = perCat.entrySet().stream()
                .filter(e -> DISCRETIONARY_CATEGORIES.contains(normalize(e.getKey())))
                .map(e -> Map.entry(e.getKey(), median(e.getValue())))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        List<Suggestion> out = new ArrayList<>();
        BigDecimal remaining = gap;
        for (Map.Entry<String, Double> e : medians) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            double base = e.getValue();
            double cutPercent = 0.2; // suggest 20% cut
            BigDecimal impact = BigDecimal.valueOf(base * cutPercent).setScale(2, RoundingMode.HALF_UP);
            String cat = e.getKey();
            if (goal.getProtectedCategories() != null && goal.getProtectedCategories().stream().map(this::normalize).collect(Collectors.toSet()).contains(cat)) {
                continue; // skip protected
            }
            Suggestion s = new Suggestion(
                    "Trim " + cat + " by ~20%",
                    "Cap this category and aim for ~$" + impact + "/month less",
                    "Based on last " + (perCat.get(cat).size()) + " months median",
                    "variable_trim",
                    impact
            );
            out.add(s);
            remaining = remaining.subtract(impact);
        }
        return out;
    }

    private Suggestion generateTimelineLeverSuggestion(GoalRequest goal) {
        int newMonths = goal.getMonthsToDeadline() + 1;
        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentSavings() == null ? BigDecimal.ZERO : goal.getCurrentSavings());
        BigDecimal newReq = remaining.divide(BigDecimal.valueOf(newMonths), 2, RoundingMode.HALF_UP);
        Suggestion s = new Suggestion(
                "Move deadline by +1 month",
                "Consider extending timeline for lower monthly requirement",
                "Spreads remaining amount over more months",
                "timeline",
                BigDecimal.ZERO
        );
        s.setNewMonthsToDeadline(newMonths);
        s.setNewRequiredMonthly(newReq);
        return s;
    }

    private Suggestion generateIncomeLeverSuggestion() {
        BigDecimal impact = BigDecimal.valueOf(100); // light-touch assumption
        return new Suggestion(
                "Add one extra shift / freelance (+$100)",
                "If feasible, add a small monthly income boost",
                "Only if income seems flexible",
                "income",
                impact
        );
    }

    private double median(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        List<Double> copy = new ArrayList<>(values);
        copy.sort(Double::compareTo);
        int n = copy.size();
        if (n % 2 == 1) return copy.get(n / 2);
        return (copy.get(n / 2 - 1) + copy.get(n / 2)) / 2.0;
    }
}

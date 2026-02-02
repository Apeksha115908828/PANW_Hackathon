package com.panw.hackathon.service;

import com.panw.hackathon.model.ForecastResult;
import com.panw.hackathon.model.GoalRequest;
import com.panw.hackathon.model.Suggestion;
import com.panw.hackathon.model.Transaction;
import com.panw.hackathon.util.GoalTextParser;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        BigDecimal targetAmount = goal.getTargetAmount();
        Integer monthsToDeadline = goal.getMonthsToDeadline();
        if ((targetAmount == null || targetAmount.compareTo(BigDecimal.ONE) < 0)
                || (monthsToDeadline == null || monthsToDeadline < 1)) {
            GoalTextParser.ParsedGoal parsed = GoalTextParser.parse(goal.getGoalText());
            if (parsed == null) {
                throw new IllegalArgumentException("Unable to parse goal text. Please include an amount (e.g., $5000) and a timeframe (e.g., in 6 months or by 2026-06-15).");
            }
            targetAmount = parsed.targetAmount;
            monthsToDeadline = parsed.monthsToDeadline;
        }

        BigDecimal requiredMonthly = targetAmount
            .divide(BigDecimal.valueOf(monthsToDeadline), 2, RoundingMode.HALF_UP);

        BigDecimal projectedMonthlyToGoal = BigDecimal.valueOf(Math.max(0, p50)).setScale(2, RoundingMode.HALF_UP);

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

        BigDecimal forecastedBalanceAtDeadlineP50 = BigDecimal.valueOf(Math.max(0, p50))
            .multiply(BigDecimal.valueOf(monthsToDeadline))
            .setScale(2, RoundingMode.HALF_UP);

        List<Suggestion> suggestions = new ArrayList<>();
        BigDecimal gap = requiredMonthly.subtract(projectedMonthlyToGoal);
        if (gap.compareTo(BigDecimal.ZERO) > 0) {
            suggestions.addAll(generateVariableTrimSuggestions(categorySpendPerMonth, gap));
            Suggestion subs = generateSubscriptionCleanupSuggestion(categorySpendPerMonth);
            if (subs != null) suggestions.add(subs);
            suggestions.add(generateTimelineLeverSuggestion(monthsToDeadline, targetAmount));
            suggestions.add(generateIncomeLeverSuggestion());

            // Optional: augment with LLM-generated suggestions if enabled
            List<Suggestion> llmSuggestions = new LLMSuggestionService().generateSuggestions(
                goal,
                categorySpendPerMonth,
                months,
                BigDecimal.valueOf(p50),
                gap
            );
            suggestions.addAll(llmSuggestions);
        }

        ForecastResult result = new ForecastResult();
        result.setStatus(status);
        result.setOnTrack(onTrack);
        result.setRequiredMonthly(requiredMonthly);
        result.setParsedTargetAmount(targetAmount);
        result.setParsedMonthsToDeadline(monthsToDeadline);
        result.setP10(BigDecimal.valueOf(p10).setScale(2, RoundingMode.HALF_UP));
        result.setP50(BigDecimal.valueOf(p50).setScale(2, RoundingMode.HALF_UP));
        result.setP90(BigDecimal.valueOf(p90).setScale(2, RoundingMode.HALF_UP));
        result.setProjectedMonthlyToGoal(projectedMonthlyToGoal);
        result.setForecastedBalanceAtDeadlineP50(forecastedBalanceAtDeadlineP50);
        result.setSuggestions(suggestions);
        result.setMonthlyGap(gap.max(BigDecimal.ZERO));
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

    private List<Suggestion> generateVariableTrimSuggestions(Map<String, List<Double>> perCat, BigDecimal gap) {
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
            double cutPercent = 0.2;
            BigDecimal impact = BigDecimal.valueOf(base * cutPercent).setScale(2, RoundingMode.HALF_UP);
                String cat = e.getKey();
            Suggestion s = new Suggestion(
                    friendlyTitleForCategory(cat),
                    behaviorTipForCategory(cat, impact),
                    "Estimate based on last " + (perCat.get(cat).size()) + " months; actual impact can vary.",
                    "variable_trim",
                    impact
            );
            out.add(s);
            remaining = remaining.subtract(impact);
        }
        return out;
    }

    private Suggestion generateSubscriptionCleanupSuggestion(Map<String, List<Double>> perCat) {
        String cat = "Subscriptions";
        List<Double> vals = perCat.get(cat);
        if (vals == null || vals.isEmpty()) return null;
        double base = median(vals);
        BigDecimal impact = BigDecimal.valueOf(Math.min(Math.max(15, base * 0.25), 30)).setScale(2, RoundingMode.HALF_UP);
        return new Suggestion(
                "Pause one low-use subscription",
                "If this feels doable, consider pausing a seldom-used subscription for a month (e.g., a secondary streaming or add-on).",
                "Based on typical subscription amounts and your recent spend; actual savings may vary. Essentials remain untouched.",
                "subscription_cleanup",
                impact
        );
    }

    private String friendlyTitleForCategory(String cat) {
        String c = normalize(cat);
        switch (c) {
            case "Dining":
            case "Restaurants":
                return "One home-cooked swap each week";
            case "Shopping":
                return "24‑hour pause before non‑essentials";
            case "Rideshare":
                return "Bundle errands; one fewer short ride";
            case "Entertainment":
                return "Skip one paid outing this month";
            case "Travel":
                return "Choose a lower‑cost local activity";
            case "Hobbies":
                return "Reuse gear; postpone one new item";
            default:
                return "One small tweak in " + c;
        }
    }

    private String behaviorTipForCategory(String cat, BigDecimal impact) {
        String c = normalize(cat);
        String dollars = "$" + impact;
        switch (c) {
            case "Dining":
            case "Restaurants":
                return "You could consider swapping one takeout/restaurant meal per week for a home‑cooked option. Rough estimate: +" + dollars + "/mo toward your goal.";
            case "Shopping":
                return "One option: add a 24‑hour pause before non‑essential online purchases and skip one item this month. Rough estimate: +" + dollars + "/mo.";
            case "Rideshare":
                return "If this feels doable, bundle errands and try one fewer short ride each week or compare a transit option for short distances. Rough estimate: +" + dollars + "/mo.";
            case "Entertainment":
                return "You could pick one paid outing or rental to skip this month and choose a low‑cost alternative. Rough estimate: +" + dollars + "/mo.";
            case "Travel":
                return "Consider a lower‑cost local activity this month instead of a discretionary trip. Rough estimate: +" + dollars + "/mo.";
            case "Hobbies":
                return "Try reusing existing gear or buying supplies in bulk once this month; postpone one new purchase. Rough estimate: +" + dollars + "/mo.";
            default:
                return "Choose one small habit change this month in this category (e.g., one fewer purchase or a lower‑cost alternative). Rough estimate: +" + dollars + "/mo.";
        }
    }

    private Suggestion generateTimelineLeverSuggestion(Integer monthsToDeadline, BigDecimal targetAmount) {
        int newMonths = monthsToDeadline + 1;
        BigDecimal remaining = targetAmount;
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
        BigDecimal impact = BigDecimal.valueOf(100);
        return new Suggestion(
            "Small income boost (optional)",
            "If this feels doable, consider one extra shift or a small freelance task this month.",
            "Only if income seems flexible; rough estimate and entirely optional.",
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

package com.panw.hackathon.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panw.hackathon.model.GoalRequest;
import com.panw.hackathon.model.Suggestion;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class LLMSuggestionService {
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Suggestion> generateSuggestions(
            GoalRequest goal,
            Map<String, List<Double>> categorySpendPerMonth,
            List<YearMonth> baselineMonths,
            BigDecimal p50,
            BigDecimal gap
    ) {
        // hard-coded here, will be moved to config file
        String apiUrl = System.getenv("LLM_API");
        String apiKey = System.getenv("LLM_KEY");
        if (apiUrl == null || apiUrl.isBlank()) {
            return Collections.emptyList();
        }

        String prompt = buildPrompt(goal, categorySpendPerMonth, baselineMonths, p50, gap);
        try {
            HttpClient client = HttpClient.newHttpClient();
            Map<String, String> body = Map.of("prompt", prompt);
            String json = mapper.writeValueAsString(body);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));
            if (apiKey != null && !apiKey.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }
            HttpRequest request = reqBuilder.build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                List<Suggestion> suggestions = mapper.readValue(resp.body(), new TypeReference<List<Suggestion>>() {});
                return suggestions == null ? Collections.emptyList() : suggestions;
            }
        } catch (IOException | InterruptedException e) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private String buildPrompt(
            GoalRequest goal,
            Map<String, List<Double>> perCat,
            List<YearMonth> baselineMonths,
            BigDecimal p50,
            BigDecimal gap
    ) {
        int historyMonths = baselineMonths.size();
        String categoryBreakdown = perCat.entrySet().stream()
                .sorted((a,b) -> Double.compare(median(b.getValue()), median(a.getValue())))
                .map(e -> e.getKey() + ": $" + String.format(Locale.US, "%.2f", median(e.getValue())))
                .collect(Collectors.joining("\n"));

        String highVariance = perCat.entrySet().stream()
                .sorted((a,b) -> Double.compare(variance(b.getValue()), variance(a.getValue())))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", "));

        String subscriptions = perCat.containsKey("Subscriptions") ? "Subscriptions present" : "None detected";

        String tpl = "Role: You are a financial wellness assistant focused on supportive, non-judgmental guidance.\n" +
                "Context:\n\n" +
                "Goal:\n" +
                "Target: $%s\n" +
                "Deadline: %d months\n" +
                "Monthly gap to close: $%s\n" +
                "Spending summary (last %d months):\n" +
                "%s\n" +
                "Detected patterns:\n" +
                "High-variance categories: %s\n" +
                "Recurring subscriptions: %s\n" +
                "User preferences:\n" +
                "Protected categories: (not used)\n" +
                "Constraints:\n\n" +
                "Avoid shaming, judgment, or absolute language.\n" +
                "Avoid generic advice like \"spend less\" or \"cap category\".\n" +
                "Do not recommend cuts to essentials (e.g., rent, groceries, utilities) unless explicitly allowed.\n" +
                "No financial or investment advice.\n" +
                "Suggest small, concrete behavior changes (e.g., \"swap one takeout meal for home‑cooked\"), framed as optional tradeoffs.\n" +
                "Quantify estimated monthly impact when possible; be transparent about uncertainty and assumptions.\n" +
                "Task:\n\n" +
                "Generate 3–5 personalized spending adjustment suggestions that could realistically help close the monthly gap.\n" +
                "Each suggestion must include:\n\n" +
                "Short, friendly title\n" +
                "Concrete behavioral change (not a category cap)\n" +
                "Estimated monthly impact (rough estimate OK)\n" +
                "Brief explanation of why it fits the user’s behavior and any assumptions\n" +
                "Optional lever metadata (e.g., \"leverType\": \"variable_trim\"|\"subscription_cleanup\"|\"income\"|\"timeline\"; if timeline, include newMonthsToDeadline and newRequiredMonthly)\n" +
                "Tone:\n\n" +
                "Supportive, practical, non-judgmental. Use wording like \"could consider\", \"one option\", \"if this feels doable\".\n" +
                "Output (JSON array of suggestions):\n" +
                "[\n  {\n    \"title\": \"...\",\n    \"action\": \"...\",\n    \"impactPerMonth\": 00.00,\n    \"rationale\": \"...\",\n    \"leverType\": \"variable_trim|subscription_cleanup|income|timeline\",\n    \"newMonthsToDeadline\": 11,\n    \"newRequiredMonthly\": 273.00\n  }\n]\n" +
                "Notes:\n\n" +
                "If protected categories include a candidate category, skip it.\n" +
                "If the gap is already ≤ 0, focus on light reinforcement/optimization.\n" +
                "Use recent spend medians and typical amounts to estimate impact; state uncertainty (\"estimate based on last N months; actual results may vary\").\n";

        return String.format(Locale.US, tpl,
            goal.getTargetAmount(),
            goal.getMonthsToDeadline(),
            gap.setScale(2, java.math.RoundingMode.HALF_UP),
            historyMonths,
            categoryBreakdown,
            highVariance,
            subscriptions
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

    private double variance(List<Double> values) {
        if (values == null || values.size() < 2) return 0.0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double var = 0.0;
        for (double v : values) var += Math.pow(v - mean, 2);
        return var / (values.size() - 1);
    }
}

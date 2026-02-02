package com.panw.hackathon.model;

import java.math.BigDecimal;
import java.util.List;

public class ForecastResult {
    private String status; // on_track | borderline | off_track
    private boolean onTrack;

    private BigDecimal requiredMonthly;

    private BigDecimal parsedTargetAmount;
    private Integer parsedMonthsToDeadline;

    private BigDecimal p10;
    private BigDecimal p50;
    private BigDecimal p90;

    private BigDecimal projectedMonthlyToGoal; // after buffer
    private BigDecimal forecastedBalanceAtDeadlineP50;

    private List<Suggestion> suggestions;

    private BigDecimal monthlyGap;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isOnTrack() { return onTrack; }
    public void setOnTrack(boolean onTrack) { this.onTrack = onTrack; }
    public BigDecimal getRequiredMonthly() { return requiredMonthly; }
    public void setRequiredMonthly(BigDecimal requiredMonthly) { this.requiredMonthly = requiredMonthly; }
    public BigDecimal getParsedTargetAmount() { return parsedTargetAmount; }
    public void setParsedTargetAmount(BigDecimal parsedTargetAmount) { this.parsedTargetAmount = parsedTargetAmount; }
    public Integer getParsedMonthsToDeadline() { return parsedMonthsToDeadline; }
    public void setParsedMonthsToDeadline(Integer parsedMonthsToDeadline) { this.parsedMonthsToDeadline = parsedMonthsToDeadline; }
    public BigDecimal getP10() { return p10; }
    public void setP10(BigDecimal p10) { this.p10 = p10; }
    public BigDecimal getP50() { return p50; }
    public void setP50(BigDecimal p50) { this.p50 = p50; }
    public BigDecimal getP90() { return p90; }
    public void setP90(BigDecimal p90) { this.p90 = p90; }
    public BigDecimal getProjectedMonthlyToGoal() { return projectedMonthlyToGoal; }
    public void setProjectedMonthlyToGoal(BigDecimal projectedMonthlyToGoal) { this.projectedMonthlyToGoal = projectedMonthlyToGoal; }
    public BigDecimal getForecastedBalanceAtDeadlineP50() { return forecastedBalanceAtDeadlineP50; }
    public void setForecastedBalanceAtDeadlineP50(BigDecimal forecastedBalanceAtDeadlineP50) { this.forecastedBalanceAtDeadlineP50 = forecastedBalanceAtDeadlineP50; }
    public List<Suggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<Suggestion> suggestions) { this.suggestions = suggestions; }
    public BigDecimal getMonthlyGap() { return monthlyGap; }
    public void setMonthlyGap(BigDecimal monthlyGap) { this.monthlyGap = monthlyGap; }
}

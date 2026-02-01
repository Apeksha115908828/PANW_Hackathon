package com.panw.hackathon.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class GoalRequest {
    @NotNull
    @Min(1)
    private BigDecimal targetAmount;

    @NotNull
    @Min(1)
    private Integer monthsToDeadline;

    @Min(0)
    private BigDecimal currentSavings = BigDecimal.ZERO;

    @Min(0)
    private BigDecimal buffer = BigDecimal.ZERO;

    private List<String> protectedCategories; // optional

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public Integer getMonthsToDeadline() { return monthsToDeadline; }
    public void setMonthsToDeadline(Integer monthsToDeadline) { this.monthsToDeadline = monthsToDeadline; }

    public BigDecimal getCurrentSavings() { return currentSavings; }
    public void setCurrentSavings(BigDecimal currentSavings) { this.currentSavings = currentSavings; }

    public BigDecimal getBuffer() { return buffer; }
    public void setBuffer(BigDecimal buffer) { this.buffer = buffer; }

    public List<String> getProtectedCategories() { return protectedCategories; }
    public void setProtectedCategories(List<String> protectedCategories) { this.protectedCategories = protectedCategories; }
}

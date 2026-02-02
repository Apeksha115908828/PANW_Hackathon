package com.panw.hackathon.model;

import java.math.BigDecimal;

public class GoalRequest {
    private BigDecimal targetAmount;
    private Integer monthsToDeadline;

    private String goalText;

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public Integer getMonthsToDeadline() { return monthsToDeadline; }
    public void setMonthsToDeadline(Integer monthsToDeadline) { this.monthsToDeadline = monthsToDeadline; }

    public String getGoalText() { return goalText; }
    public void setGoalText(String goalText) { this.goalText = goalText; }
}

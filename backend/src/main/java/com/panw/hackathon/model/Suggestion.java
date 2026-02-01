package com.panw.hackathon.model;

import java.math.BigDecimal;

public class Suggestion {
    private String title;
    private String action;
    private String rationale;
    private String leverType; // variable_trim, subscription_cleanup, income, timeline
    private BigDecimal impactPerMonth = BigDecimal.ZERO; // $/month

    // Optional: for timeline lever
    private Integer newMonthsToDeadline;
    private BigDecimal newRequiredMonthly;

    public Suggestion() {}

    public Suggestion(String title, String action, String rationale, String leverType, BigDecimal impactPerMonth) {
        this.title = title;
        this.action = action;
        this.rationale = rationale;
        this.leverType = leverType;
        this.impactPerMonth = impactPerMonth;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getLeverType() { return leverType; }
    public void setLeverType(String leverType) { this.leverType = leverType; }
    public BigDecimal getImpactPerMonth() { return impactPerMonth; }
    public void setImpactPerMonth(BigDecimal impactPerMonth) { this.impactPerMonth = impactPerMonth; }
    public Integer getNewMonthsToDeadline() { return newMonthsToDeadline; }
    public void setNewMonthsToDeadline(Integer newMonthsToDeadline) { this.newMonthsToDeadline = newMonthsToDeadline; }
    public BigDecimal getNewRequiredMonthly() { return newRequiredMonthly; }
    public void setNewRequiredMonthly(BigDecimal newRequiredMonthly) { this.newRequiredMonthly = newRequiredMonthly; }
}

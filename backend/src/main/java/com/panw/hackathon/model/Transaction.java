package com.panw.hackathon.model;

import java.time.LocalDate;

public class Transaction {
    private LocalDate date;
    private double amount; // positive = inflow, negative = outflow
    private String merchant;
    private String category;
    private String account;

    public Transaction() {}

    public Transaction(LocalDate date, double amount, String merchant, String category, String account) {
        this.date = date;
        this.amount = amount;
        this.merchant = merchant;
        this.category = category;
        this.account = account;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
}

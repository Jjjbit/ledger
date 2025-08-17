package com.ledger.ledger.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BudgetManager {
    /*private BigDecimal used;
    private BigDecimal totalBudget;
    private BigDecimal remaining=totalBudget.subtract(used);
    private BigDecimal isOverBudget;*/
    private List<Budget> budgets;

    public BudgetManager(List<Budget> budgets) {
        this.budgets = budgets;
    }

    //total budget for a user for a specific period and without category
    public BigDecimal getUserTotalBudget(User user, Budget.Period period) {
        return budgets.stream()
                .filter(b -> b.getOwner().equals(user))
                .filter(b-> !b.isForCategory())
                .filter(b -> b.getPeriod().equals(period))
                .filter(b -> b.isInPeriod(LocalDate.now()))
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    //total budget for a user for a specific period and category
    public BigDecimal getCategoryBudgets(User user, Budget.Period period, CategoryComponent category) {
        return budgets.stream()
                .filter(b -> b.getOwner().equals(user))
                .filter(b -> category.equals(b.getCategory()))
                .filter(b -> b.getPeriod().equals(period))
                .filter(b -> b.isInPeriod(LocalDate.now()))
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    //total spending for a user for a specific period and category
    public BigDecimal getCategorySpending(User user, Budget.Period period, CategoryComponent category) {
        /*List<Transaction> transactions = category.getTransactions().stream()
                .filter(t -> budgets.stream()
                        .anyMatch(b -> b.getOwner().equals(user) && b.getCategory().equals(category) && b.isTransactionInPeriod(t, period)))
                .toList();
        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);*/
        return budgets.stream()
                .filter(b -> b.getOwner().equals(user))
                .filter(b -> b.getCategory().equals(category))
                .filter(b -> b.getPeriod().equals(period))
                .filter(b -> b.isInPeriod(LocalDate.now()))
                .flatMap(b -> b.getCategory().getTransactions().stream())
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    //total spending for a user for a specific period
    public BigDecimal getTotalSpending(User user, Budget.Period period) {
        return budgets.stream()
                .filter(b -> b.getOwner().equals(user))
                .filter(b -> !b.isForCategory())
                .filter(b -> b.getPeriod().equals(period))
                .filter(b -> b.isInPeriod(LocalDate.now()))
                .flatMap(b -> b.getCategory().getTransactions().stream())
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Check if the user is over budget for a specific period and category
    public boolean isOverBudgetForCategory(User user, Budget.Period period, CategoryComponent category) {
        BigDecimal budgetAmount = getCategoryBudgets(user, period, category);
        BigDecimal spending = getCategorySpending(user, period, category);
        return spending.compareTo(budgetAmount) > 0;
    }

    // Check if the user is over budget for a specific period
    public boolean isOverBudget(User user, Budget.Period period) {
        BigDecimal budgetAmount = getUserTotalBudget(user, period);
        BigDecimal spending = getTotalSpending(user, period);
        return spending.compareTo(budgetAmount) > 0;
    }


}

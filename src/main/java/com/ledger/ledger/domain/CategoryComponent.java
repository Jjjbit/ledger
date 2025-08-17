package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "level", discriminatorType = DiscriminatorType.STRING)
public abstract class CategoryComponent {
    public enum CategoryType {
        INCOME, EXPENSE, ROOT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    protected String name;

    @Column(length = 20, nullable = false)
    protected CategoryType type; //"income", "expense", "root"

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<Transaction> transactions = new ArrayList<>(); //un category -> più transazioni.

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Budget> budgets = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "parent_id")
    protected CategoryComponent parent;

    public CategoryComponent() {}
    public CategoryComponent(String name, CategoryType type) {
        this.name = name;
        this.type = type;
    }
    public abstract void remove(CategoryComponent child);
    public abstract void add(CategoryComponent child);
    public abstract List<CategoryComponent> getChildren();
    public abstract void display(String indent);
    public CategoryComponent getParent(){
        return this.parent;
    }
    public void setParent(CategoryComponent parent) {
        this.parent = parent;
    }
    public String getName() {
        return name;
    }
    public CategoryType getType() {
        return type;
    }
    //public abstract void changeLevel(CategoryComponent root);

    public void addTransaction(Transaction t) {
        transactions.add(t);
    }
    public void addBudget(Budget b) {
        budgets.add(b);
    }

    //returns list of transactions of the category and its subcategories in period
    public List<Transaction> getTransactionsInPeriod(Budget.Period period, LocalDate startDate) {
        return transactions.stream()
                .filter(t -> switch (period) {
                    case MONTHLY -> t.getDate().isBefore(startDate.plusMonths(1));
                    case WEEKLY -> t.getDate().isBefore(startDate.plusWeeks(1));
                    case YEARLY -> t.getDate().isBefore(startDate.plusYears(1));
                })
                .toList();
    }

    //returns list of budget for the category and its subcategories in period
    public List<Budget> getBudgetsForPeriod(Budget.Period p) {
        return budgets.stream()
                .filter(b -> b.getPeriod() == p)
                .toList();
    }

    //returns total budget for the category and its subcategories in period
    public BigDecimal getTotalBudgetForPeriod(Budget.Period period) {
        return budgets.stream()
                .filter(b -> b.getPeriod() == period)
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public abstract List<Transaction> getTransactions();

    public abstract void printTransactionSummary();
}

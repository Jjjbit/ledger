package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "level", discriminatorType = DiscriminatorType.STRING)
public abstract class LedgerCategoryComponent {

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
    protected LedgerCategoryComponent parent;

    @ManyToOne
    @JoinColumn(name = "ledger_id")
    private Ledger ledger;

    public LedgerCategoryComponent() {}
    public LedgerCategoryComponent(String name, CategoryType type) {
        this.name = name;
        this.type = type;
    }

    public abstract void remove(LedgerCategoryComponent child);
    public abstract void add(LedgerCategoryComponent child);
    public abstract List<LedgerCategoryComponent> getChildren();
    public abstract void display(String indent);
    public LedgerCategoryComponent getParent(){
        return this.parent;
    }
    public void setParent(LedgerCategoryComponent parent) {
        this.parent = parent;
    }
    public void setLedger (Ledger ledger){this.ledger=ledger;}
    public void setName(String name){this.name=name;}
    public String getName() {
        return name;
    }
    public CategoryType getType() {
        return type;
    }
    public Long getId() {
        return id;
    }
    //public abstract void changeLevel(CategoryComponent root);

    public void addTransaction(Transaction t) {
        transactions.add(t);
    }
    public void removeTransaction(Transaction t) {
        transactions.remove(t);
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

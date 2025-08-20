package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Ledger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length =50, nullable= false, unique = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "ledger", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions=new ArrayList<>(); //relazione tra Transaction e Ledger è composizione

    @OneToMany(mappedBy = "ledger", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LedgerCategoryComponent> categories = new ArrayList<>();

    @OneToMany(mappedBy = "ledger", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<BorrowingAndLending> borrowingAndLendings = new ArrayList<>(); // relazione tra LoanRecord e Ledger è aggregazione

    @Column(name = "total_income", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalIncome = BigDecimal.ZERO; // Totale entrate del ledger

    @Column(name = "total_expenses", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalExpenses = BigDecimal.ZERO; // Totale uscite del ledger

    public Ledger() {}
    public Ledger(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    public String getName(){return this.name;}
    public User getOwner(){return this.owner;}
    public void addTransaction(Transaction tx) {
        transactions.add(tx); // Aggiunge una transazione al ledger
        tx.execute();
    }
    public List<Transaction> getTransactions() {
        return transactions;
    }
    public List<LedgerCategoryComponent> getCategories(){return categories;}

    public void removeTransaction(Transaction tx) {
        transactions.remove(tx); // Rimuove una transazione dal ledger
    }
    public void addLoanRecord(BorrowingAndLending loanRecord) {
        borrowingAndLendings.add(loanRecord);
    }
    public Long getId() {
        return id;
    }


    public void addCategory(LedgerCategoryComponent category) {
        categories.add(category);
        category.setLedger(this);
    }

}

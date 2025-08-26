package com.ledger.ledger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("Category")
public class LedgerCategory extends LedgerCategoryComponent{

    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval = true)
    private List<LedgerCategoryComponent> children = new ArrayList<>();

    public LedgerCategory() {}
    public LedgerCategory(String name, CategoryType type, Ledger ledger) {
        super(name, type, ledger);
    }

    @Override
    public void remove(LedgerCategoryComponent child) {
        children.remove(child);
    }

    @Override
    public void add(LedgerCategoryComponent child) {// Aggiunge una SubCategory a Category
        if (child.type == this.type) {
            children.add(child);
            child.setParent(this);

        } else {
            throw new IllegalArgumentException("Invalid category hierarchy");
        }
    }

    @Override
    public List<LedgerCategoryComponent> getChildren() {
        return children;
    }

    @Override
    // Ritorna la lista delle transazioni di questa categoria e delle sue subcategorie
    public List<Transaction> getTransactions() {
        for (LedgerCategoryComponent child : children) {
            this.transactions.addAll(child.getTransactions());
        }
        return this.transactions;
    }

    //stampa un riepilogo delle transazioni della categoria in ordine decrescente di data
    @Override
    public void printTransactionSummary() {
        this.transactions = getTransactions();
        System.out.println("Transaction summary for: " + name);
        for (Transaction t : this.transactions) {
            System.out.println(t.getDate() + " - " + t.getFromAccount() + " - " + t.getAmount() + " - " + t.getNote());
        }
    }

    public void display(String indent) {
        System.out.println(indent + "- " + name + " (" + type + ")");
        for (LedgerCategoryComponent child : children) {
            child.display(indent + "  ");
        }
    }

}

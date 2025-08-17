package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@DiscriminatorValue("SubCategory")
public class SubCategory extends CategoryComponent {

    public SubCategory() {}
    public SubCategory(String name, CategoryType type) {
        super(name, type);
    }

    @Override
    public void remove(CategoryComponent child) {
        throw new UnsupportedOperationException("SubCategory does not support remove operation");
    }

    @Override
    public void add(CategoryComponent child) {
        throw new UnsupportedOperationException("SubCategory does not support add operation");
    }

    @Override
    @Transient
    public List<CategoryComponent> getChildren() {
        throw new UnsupportedOperationException("SubCategory does not support getChildren operation");
    }

    @Override
    public void display(String indent) {
        System.out.println(indent + "- " + name + " (" + type + ")");
    }

    @Override
    @Transient
    public List<Transaction> getTransactions() {
        return transactions.stream()
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public void printTransactionSummary() {
        this.transactions= getTransactions();
        System.out.println("Transaction summary for: " + name);
        for (Transaction t : transactions) {
            System.out.println(t.getDate() + " - " + t.getAccount() + "-" + t.getAmount() + " - " + t.getNote());
        }
    }

    public void changeLevel(CategoryComponent root) {
        if( this.getParent() != null) {
            this.getParent().remove(this);
            root.add(this);
        }

    }

    public void changeParent(CategoryComponent newParent) {
        if (this.parent != null) {
            this.parent.remove(this); // Rimuove se già ha un parent
        }
        newParent.add(this); // Aggiunge al nuovo parent
        this.parent = newParent; // Imposta il nuovo parent
    }

}

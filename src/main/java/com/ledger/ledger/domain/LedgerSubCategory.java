package com.ledger.ledger.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import java.util.List;

@Entity
@DiscriminatorValue("SubCategory")
public class LedgerSubCategory extends LedgerCategoryComponent{

    public LedgerSubCategory() {}
    public LedgerSubCategory(String name, CategoryType type, Ledger ledger) {
        super(name, type, ledger);
    }

    @Override
    public void remove(LedgerCategoryComponent child) {
        throw new UnsupportedOperationException("SubCategory does not support remove operation");
    }

    @Override
    public void add(LedgerCategoryComponent child) {
        throw new UnsupportedOperationException("SubCategory does not support add operation");
    }

    @Override
    @Transient
    public List<LedgerCategoryComponent> getChildren() {
        throw new UnsupportedOperationException("SubCategory does not support getChildren operation");
    }

    @Override
    public void display(String indent) {
        System.out.println(indent + "- " + name + " (" + type + ")");
    }

    @Override
    public void printTransactionSummary() {
        this.transactions= getTransactions();
        System.out.println("Transaction summary for: " + name);
        for (Transaction t : transactions) {
            System.out.println(t.getDate() + " - " + t.getFromAccount() + "-" + t.getAmount() + " - " + t.getNote());
        }
    }
}

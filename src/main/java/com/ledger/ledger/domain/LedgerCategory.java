package com.ledger.ledger.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@DiscriminatorValue("Category")
public class LedgerCategory extends LedgerCategoryComponent{

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LedgerCategoryComponent> children = new ArrayList<>();

    public LedgerCategory() {}
    public LedgerCategory(String name, CategoryType type) {
        super(name, type);
    }

    public void changeLevel(LedgerCategoryComponent root, LedgerCategoryComponent parent) {
        if (this instanceof LedgerCategory && this.getChildren().isEmpty()) {
            LedgerSubCategory sub = new LedgerSubCategory(this.name, this.type);
            sub.transactions.addAll(this.getTransactions()); // Copia le transazioni dalla categoria alla subcategoria
            parent.add(sub);
            root.remove(this);
        }else {
            System.out.println("Cannot demote a category with subcategory.");
        }
    }

    @Override
    public void remove(LedgerCategoryComponent child) {
        children.remove(child);
    }

    @Override
    public void add(LedgerCategoryComponent child) { // Aggiunge una SubCategory a Category
        if (this.type == CategoryType.ROOT &&
                (child.type == CategoryType.INCOME || child.type == CategoryType.EXPENSE)) {
            children.add(child);
            child.setParent(this);
        } else if (this.type != CategoryType.ROOT && child.type == this.type) {
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

    // Ritorna la lista delle transazioni di questa categoria e delle sue subcategorie in ordine decrescente di data
    @Override
    public List<Transaction> getTransactions() {
        List<Transaction> all = new ArrayList<>(this.transactions);
        for (LedgerCategoryComponent child : children) {
            all.addAll(child.getTransactions());
        }
        return all.stream()
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .collect(Collectors.toList());
    }

    //stampa un riepilogo delle transazioni della categoria in ordine decrescente di data
    @Override
    public void printTransactionSummary() {
        this.transactions = getTransactions();
        System.out.println("Transaction summary for: " + name);
        for (Transaction t : this.transactions) {
            System.out.println(t.getDate() + " - " + t.getAccount() + " - " + t.getAmount() + " - " + t.getNote());
        }
    }

    @Override
    public LedgerCategoryComponent getParent() {
        if(this.type== CategoryType.ROOT) {
            return null;
        }else{
            return this.parent;
        }
    }
    public void display(String indent) {
        System.out.println(indent + "- " + name + " (" + type + ")");
        for (LedgerCategoryComponent child : children) {
            child.display(indent + "  ");
        }
    }

}

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


    public void changeParent(CategoryComponent newParent) {
        this.getParent().getChildren().remove(this); // Rimuove se già ha un parent
        newParent.add(this); // Aggiunge al nuovo parent
        this.parent = newParent; // Imposta il nuovo parent
    }


}

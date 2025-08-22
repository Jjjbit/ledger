package com.ledger.ledger.domain;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.html.IThrowableRenderer;
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
public class Category extends CategoryComponent {

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryComponent> children = new ArrayList<>();

    public Category() {}
    public Category(String name, CategoryType type) {
        super(name, type);
    }

    @Override
    public void remove(CategoryComponent child) {
        children.remove(child);
    }

    @Override
    public void add(CategoryComponent child) { // Aggiunge una SubCategory a Category
        if (child.type == this.type) {
            children.add(child);
            child.setParent(this);
        } else {
            throw new IllegalArgumentException("Invalid category hierarchy");
        }
    }

    @Override
    public List<CategoryComponent> getChildren() {
        return children;
    }

    public void display(String indent) {
        System.out.println(indent + "- " + name + " (" + type + ")");
        for (CategoryComponent child : children) {
            child.display(indent + "  ");
        }
    }

    @Override
    public void setParent(CategoryComponent parent) {
        if (parent != null) {
            throw new UnsupportedOperationException("Root category cannot have a parent.");
        }
        super.setParent(null);
    }

}

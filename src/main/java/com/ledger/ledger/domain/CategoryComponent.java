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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    protected String name;

    @Column(length = 20, nullable = false)
    protected CategoryType type; //"income", "expense"

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

    public CategoryComponent getParent() {
        return parent;
    }

    public void setParent(CategoryComponent parent) {
        this.parent = parent;
    }
    public String getName() {
        return name;
    }
    public void setName(String name){this.name=name;}
    public CategoryType getType() {
        return type;
    }
    public Long getId() {
        return id;
    }
}

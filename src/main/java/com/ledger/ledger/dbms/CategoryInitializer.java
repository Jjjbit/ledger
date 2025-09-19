package com.ledger.ledger.dbms;

import com.ledger.ledger.domain.Category;
import com.ledger.ledger.domain.CategoryComponent;
import com.ledger.ledger.domain.CategoryType;
import com.ledger.ledger.repository.CategoryComponentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CategoryInitializer {
    @Autowired
    private CategoryComponentRepository categoryComponentRepository;

    @PostConstruct
    public void init() {
        if (categoryComponentRepository.count() == 0) {

            CategoryComponent transport = new Category("Transport", CategoryType.EXPENSE);
            CategoryComponent salary = new Category("Salary", CategoryType.INCOME);
            CategoryComponent freelance = new Category("Freelance", CategoryType.INCOME);
            CategoryComponent entertainment = new Category("Entertainment", CategoryType.EXPENSE);
            CategoryComponent utilities = new Category("Utilities", CategoryType.EXPENSE);
            CategoryComponent health = new Category("Health", CategoryType.EXPENSE);
            CategoryComponent education = new Category("Education", CategoryType.EXPENSE);
            CategoryComponent shopping = new Category("Shopping", CategoryType.EXPENSE);
            CategoryComponent gifts = new Category("Gifts", CategoryType.EXPENSE);


            categoryComponentRepository.save(transport);
            categoryComponentRepository.save(salary);
            categoryComponentRepository.save(freelance);
            categoryComponentRepository.save(entertainment);
            categoryComponentRepository.save(utilities);
            categoryComponentRepository.save(health);
            categoryComponentRepository.save(education);
            categoryComponentRepository.save(shopping);
            categoryComponentRepository.save(gifts);


        }
    }
}

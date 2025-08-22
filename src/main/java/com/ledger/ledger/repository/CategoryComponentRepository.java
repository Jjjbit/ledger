package com.ledger.ledger.repository;

import com.ledger.ledger.domain.Category;
import com.ledger.ledger.domain.CategoryComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryComponentRepository extends JpaRepository<CategoryComponent, Long> {
    List<Category> findByParentIsNull();
    CategoryComponent findByName(String name);
}

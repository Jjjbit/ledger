package com.ledger.ledger.repository;

import com.ledger.ledger.domain.CategoryComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryComponentRepository extends JpaRepository<CategoryComponent, Long> {

}

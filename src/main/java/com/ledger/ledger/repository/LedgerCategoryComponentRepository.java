package com.ledger.ledger.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ledger.ledger.domain.LedgerCategoryComponent;

@Repository
public interface LedgerCategoryComponentRepository extends JpaRepository<LedgerCategoryComponent, Long> {
}

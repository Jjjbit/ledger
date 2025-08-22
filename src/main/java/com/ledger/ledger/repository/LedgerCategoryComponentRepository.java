package com.ledger.ledger.repository;

import com.ledger.ledger.domain.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ledger.ledger.domain.LedgerCategoryComponent;

@Repository
public interface LedgerCategoryComponentRepository extends JpaRepository<LedgerCategoryComponent, Long> {
    LedgerCategoryComponent findByName(String name);
    boolean existsByLedgerAndName(Ledger ledger, String name);
    LedgerCategoryComponent findByLedgerAndName(Ledger ledger,  String name);
}

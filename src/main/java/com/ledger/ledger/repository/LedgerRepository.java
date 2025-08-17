package com.ledger.ledger.repository;

import com.ledger.ledger.domain.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {
}

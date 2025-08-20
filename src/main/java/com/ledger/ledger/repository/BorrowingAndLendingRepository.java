package com.ledger.ledger.repository;

import com.ledger.ledger.domain.BorrowingAndLending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowingAndLendingRepository extends JpaRepository<BorrowingAndLending, Long> {
}

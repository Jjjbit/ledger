package com.ledger.ledger.repository;

import com.ledger.ledger.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByLedgerId(Long ledgerId);
    List<Transaction> findByCategoryId(Long categoryId);
}

package com.ledger.ledger.repository;

import com.ledger.ledger.domain.Account;
import com.ledger.ledger.domain.AccountCategory;
import com.ledger.ledger.domain.AccountType;
import com.ledger.ledger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwner(User owner);
    List<Account> findByType(AccountType type);
    List<Account> findByCategory(AccountCategory category);
    Account findByName(String name);
}

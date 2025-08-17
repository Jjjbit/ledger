package com.ledger.ledger.business;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.AccountRepository;
import com.ledger.ledger.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;



    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        return accountRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void deleteAccount(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        accountRepository.deleteById(id);
        User owner = account.getOwner();
        userRepository.save(owner);
    }

    @PutMapping("/{id}/hide")
    @Transactional
    public void hideAccount(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.hide();
        accountRepository.save(account);
        userRepository.save(account.getOwner());
    }

    @PutMapping("/{id}/includeInNetAsset")
    @Transactional
    public void setIncludedInNetAsset(@PathVariable Long id, @RequestParam boolean included) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.setIncludedInNetAsset(included);
        accountRepository.save(account);
        userRepository.save(account.getOwner());
    }

    @PutMapping("/{id}/selectable")
    public void setSelectable(@PathVariable Long id, @RequestParam boolean selectable) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.setSelectable(selectable);
        accountRepository.save(account);
    }

    @PutMapping("/{id}/credit")
    @Transactional
    public void creditAccount(@PathVariable Long id, @RequestParam BigDecimal amount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.credit(amount);
        accountRepository.save(account);
        userRepository.save(account.getOwner());
    }

    @PutMapping("/{id}/debit")
    @Transactional
    public void debitAccount(@PathVariable Long id, @RequestParam BigDecimal amount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.debit(amount);
        accountRepository.save(account);
        userRepository.save(account.getOwner());
    }

    // Change account details
    @PutMapping("{id}/update")
    @Transactional
    public void updateAccount(@PathVariable Long id, @RequestBody Account updatedAccount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.setName(updatedAccount.getName());
        account.setBalance(updatedAccount.getBalance());
        account.setNotes(updatedAccount.getNotes());
        account.setIncludedInNetAsset(updatedAccount.getIncludedInNetAsset());
        account.setSelectable(updatedAccount.getSelectable());
        accountRepository.save(account);
        userRepository.save(account.getOwner());
    }

    @GetMapping("{id}/get-transacitons-for-month")
    public List<Transaction> getAccountTransactionsForMonth(@PathVariable Long id, @RequestParam YearMonth month) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return account.getTransactionsForMonth(month);
    }

}

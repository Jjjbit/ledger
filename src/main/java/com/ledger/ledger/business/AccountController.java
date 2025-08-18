package com.ledger.ledger.business;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.AccountRepository;
import com.ledger.ledger.repository.TransactionRepository;
import com.ledger.ledger.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;

import java.security.Principal;
import java.time.MonthDay;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private EntityManager entityManager;

    @PostMapping("/create-basic-account")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> createBasicAccount(@RequestParam String accountName,
                                                     @RequestParam BigDecimal balance,
                                                     Principal principal,
                                                     @RequestParam String note,
                                                     @RequestParam boolean includedInNetWorth,
                                                     @RequestParam boolean selectable,
                                                     @RequestParam AccountType type,
                                                     @RequestParam AccountCategory category) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        BasicAccount account = new BasicAccount(
                accountName,
                balance,
                note,
                includedInNetWorth,
                selectable,
                type,
                category,
                user);
        accountRepository.save(account);
        return ResponseEntity.ok("Basic account created successfully");
    }

    @PostMapping("/create-credit-account")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> createCreditAccount(@RequestParam String accountName,
                                                      @RequestParam BigDecimal balance,
                                                      Principal principal,
                                                      @RequestParam String note,
                                                      @RequestParam boolean includedInNetWorth,
                                                      @RequestParam boolean selectable,
                                                      @RequestParam BigDecimal creditLimit,
                                                      @RequestParam BigDecimal currentDebt,
                                                      @RequestParam MonthDay billDate,
                                                      @RequestParam MonthDay dueDate,
                                                      @RequestParam AccountType type) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        CreditAccount account = new CreditAccount(
                accountName,
                balance,
                user,
                note,
                includedInNetWorth,
                selectable,
                creditLimit,
                currentDebt,
                billDate,
                dueDate,
                type);
        accountRepository.save(account);
        return ResponseEntity.ok("Credit account created successfully");
    }

    @PostMapping("/create-loan-account")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> createLoanAccount(@RequestParam String accountName,
                                                    @RequestParam String note,
                                                    @RequestParam boolean includedInNetAsset,
                                                    Principal principal,
                                                    @RequestParam int totalPeriods,
                                                    @RequestParam int repaidPeriods,
                                                    @RequestParam BigDecimal annualInterestRate,
                                                    @RequestParam BigDecimal loanAmount,
                                                    @RequestParam Long receivingAccountId,
                                                    @RequestParam MonthDay repaymentDate,
                                                    @RequestParam LoanAccount.RepaymentType repaymentType) {
        User owner = userRepository.findByUsername(principal.getName());
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        Account receivingAccount = accountRepository.findById(receivingAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Receiving account not found"));
        LoanAccount account = new LoanAccount(
                accountName,
                owner,
                note,
                includedInNetAsset,
                totalPeriods,
                repaidPeriods,
                annualInterestRate,
                loanAmount,
                receivingAccount,
                repaymentDate,
                repaymentType);
        accountRepository.save(account);
        userRepository.save(owner);
        return ResponseEntity.ok("Loan account created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        return accountRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id,
                              @RequestParam boolean deleteTransactions,
                              Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot delete someone else's account");
        }
        if (deleteTransactions) {
            // Delete all transactions associated with the account
            List<Transaction> transactionsToDelete = new ArrayList<>(account.getTransactions());
            for (Transaction transaction : transactionsToDelete) {
                transaction.setAccount(null);
                transaction.setLedger(null);
                transaction.setCategory(null);
                transactionRepository.save(transaction);
                transactionRepository.delete(transaction);
                user.setTotalAssets(
                        user.calculateTotalAssets().subtract(account.getBalance())
                );
            }
            account.getTransactions().clear();
            accountRepository.delete(account);
            return ResponseEntity.ok("Account and associated transactions deleted successfully");
        } else {
            // If not deleting transactions, just disassociate them
            for (Transaction transaction : account.getTransactions()) {
                transaction.setAccount(null);
                transactionRepository.save(transaction);
                user.setTotalAssets(
                        user.calculateTotalAssets().subtract(account.getBalance())
                                .add(account.getTransactions().stream()
                                        .filter(tx -> tx.getType() == TransactionType.INCOME)
                                        .map(Transaction::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                                .subtract(account.getTransactions().stream()
                                        .filter(tx -> tx.getType() == TransactionType.EXPENSE)
                                        .map(Transaction::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                                .subtract(account.getTransactions().stream()
                                        .filter(tx -> tx instanceof Transfer)
                                        .filter(tx -> ((Transfer) tx).getToAccount().equals(account))
                                        .map(Transaction::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                );
            }
            accountRepository.delete(account);
            return ResponseEntity.ok("Account disassociated from transactions and deleted successfully");
        }
    }

    @PutMapping("/{id}/hide")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> hideAccount(@PathVariable Long id,
                                              Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot edit someone else's account");
        }

        account.hide();
        accountRepository.save(account);
        return ResponseEntity.ok("Account hidden successfully");
    }

    @PutMapping("/{id}/edit-basic-account")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> editBasicAccount(@PathVariable Long id,
                                              Principal principal,
                                              @RequestParam String name,
                                              @RequestParam BigDecimal balance,
                                              @RequestParam String notes,
                                              @RequestParam boolean includedInNetAsset,
                                              @RequestParam boolean selectable) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!account.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot edit someone else's account");
        }
        account.setName(name);
        account.setBalance(balance);
        account.setNotes(notes);
        account.setIncludedInNetAsset(includedInNetAsset);
        account.setSelectable(selectable);
        accountRepository.save(account);
        return ResponseEntity.ok("Account edited successfully");
    }
    @PutMapping("/{id}/edit-credit-account")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> editCreditAccount(@PathVariable Long id,
                                              Principal principal,
                                              @RequestParam String name,
                                              @RequestParam BigDecimal balance,
                                              @RequestParam String notes,
                                              @RequestParam boolean includedInNetAsset,
                                              @RequestParam boolean selectable,
                                              @RequestParam BigDecimal creditLimit,
                                              @RequestParam BigDecimal currentDebt,
                                              @RequestParam MonthDay billDate,
                                              @RequestParam MonthDay dueDate) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!account.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot edit someone else's account");
        }
        if (!(account instanceof CreditAccount)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Account is not a credit account");
        }
        CreditAccount creditAccount = (CreditAccount) account;
        creditAccount.setName(name);
        creditAccount.setBalance(balance);
        creditAccount.setNotes(notes);
        creditAccount.setIncludedInNetAsset(includedInNetAsset);
        creditAccount.setSelectable(selectable);
        creditAccount.setCreditLimit(creditLimit);
        creditAccount.setCurrentDebt(currentDebt);
        creditAccount.setBillDate(billDate);
        creditAccount.setDueDate(dueDate);
        accountRepository.save(creditAccount);
        return ResponseEntity.ok("Credit account edited successfully");
    }




    @PutMapping("/{id}/includeInNetAsset")
    @Transactional
    public void setIncludedInNetAsset(@PathVariable Long id,
                                      @RequestParam boolean included) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.setIncludedInNetAsset(included);
        accountRepository.save(account);
        userRepository.save(account.getOwner());
    }

    @PutMapping("/{id}/selectable")
    public void setSelectable(@PathVariable Long id,
                              @RequestParam boolean selectable) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.setSelectable(selectable);
        accountRepository.save(account);
    }

    @PutMapping("/{id}/credit")
    @Transactional
    public void creditAccount(@PathVariable Long id,
                              @RequestParam BigDecimal amount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.credit(amount);
        accountRepository.save(account);
        userRepository.save(account.getOwner());
    }

    @PutMapping("/{id}/debit")
    @Transactional
    public void debitAccount(@PathVariable Long id,
                             @RequestParam BigDecimal amount) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.debit(amount);
        accountRepository.save(account);
        userRepository.save(account.getOwner());
    }

    // Change account details
    @PutMapping("{id}/update")
    @Transactional
    public void updateAccount(@PathVariable Long id,
                              @RequestBody Account updatedAccount) {
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

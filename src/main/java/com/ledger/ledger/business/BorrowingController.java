package com.ledger.ledger.business;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/borrowings")
public class BorrowingController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private LedgerRepository ledgerRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @PostMapping("/add-borrowing")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> addBorrowing(@RequestParam Long ledgerId,
                                               @RequestParam Long toAccountId,
                                               @RequestParam String description,
                                               @RequestParam BigDecimal amount,
                                               Principal principal,
                                               @RequestParam boolean includeInAssets,
                                               @RequestParam boolean selectable,
                                               @RequestParam String name) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }

        Ledger ledger = ledgerRepository.findById(ledgerId).orElse(null);
        if (ledger == null || !ledger.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Ledger not found or access denied");
        }
        Account toAccount = accountRepository.findById(toAccountId).orElse(null);
        if (toAccount == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid toAccountId");
        }
        BorrowingAccount borrowingAccount = new BorrowingAccount(name, amount, description, includeInAssets, selectable, user);
        Transaction borrowingTransaction = new Transfer(LocalDate.now(), "refill borrowing", borrowingAccount, toAccount, amount, ledger);
        toAccount.credit(amount);
        borrowingAccount.getOutgoingTransactions().add(borrowingTransaction);
        toAccount.getIncomingTransactions().add(borrowingTransaction);
        ledger.getTransactions().add(borrowingTransaction);
        accountRepository.save(toAccount);
        accountRepository.save(borrowingAccount);
        transactionRepository.save(borrowingTransaction);
        return ResponseEntity.ok("Borrowing added successfully");

    }

    @PutMapping("/{id}/hide")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> hideBorrowing(@PathVariable Long id,
                                                Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }
        Account account = accountRepository.findById(id).orElse(null);
        if (account == null || !(account instanceof BorrowingAccount) || !account.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Borrowing account not found or access denied");
        }
        account.hide();
        accountRepository.save(account);
        return ResponseEntity.ok("Borrowing account hidden successfully");
    }

    @DeleteMapping("/{id}/delete")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deleteBorrowing(@PathVariable Long id,
                                                  Principal principal,
                                                  @RequestParam boolean deleteRecord) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }
        Account account = accountRepository.findById(id).orElse(null);
        if (account == null || !(account instanceof BorrowingAccount) || !account.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Borrowing account not found or access denied");
        }
        if (!account.getIncomingTransactions().isEmpty() || !account.getOutgoingTransactions().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot delete borrowing account with transactions");
        }

        if (deleteRecord) {// Delete all transactions associated with the account
            List<Transaction> transactionsToDelete =
                    transactionRepository.findAll().stream()
                            .filter(tx -> (tx.getFromAccount() != null && tx.getFromAccount().equals(account)) ||
                                    ( tx.getToAccount() != null && tx.getToAccount().equals(account)))
                            .collect(Collectors.toList());

            for (Transaction transaction : transactionsToDelete) {

                if (transaction.getFromAccount() != null) {
                    transaction.getFromAccount().getOutgoingTransactions().remove(transaction);
                    transaction.setFromAccount(null);
                }

                if (transaction.getToAccount() != null) {
                    transaction.getToAccount().getIncomingTransactions().remove(transaction);
                    transaction.setToAccount(null);
                }

                if (transaction.getLedger() != null) {
                    transaction.getLedger().getTransactions().remove(transaction);
                    transaction.setLedger(null);
                }

                if (transaction.getCategory() != null){
                    transaction.getCategory().getTransactions().remove(transaction);
                    transaction.setCategory(null);
                }

                transactionRepository.delete(transaction);

            }

            accountRepository.delete(account);
            user.getAccounts().remove(account);
            userRepository.save(user);
            return ResponseEntity.ok("BorrowingAccount and associated transactions deleted successfully");
        } else {
            // If not deleting transactions, just disassociate them
            List<Transaction> transactionsToDisassociate =
                    transactionRepository.findAll().stream()
                            .filter(tx -> (tx.getFromAccount() != null && tx.getFromAccount().equals(account)) ||
                                    ( tx.getToAccount() != null && tx.getToAccount().equals(account)))
                            .collect(Collectors.toList());
            for (Transaction transaction : transactionsToDisassociate) {
                if(transaction.getFromAccount() != null) {
                    transaction.setFromAccount(null);
                }

                if (transaction.getToAccount() != null) {
                    transaction.setToAccount(null);
                }
                transactionRepository.save(transaction);
            }

            accountRepository.delete(account);
            user.getAccounts().remove(account);
            userRepository.save(user);
            return ResponseEntity.ok("BorrowingAccount disassociated from transactions and deleted successfully");
        }
    }




}

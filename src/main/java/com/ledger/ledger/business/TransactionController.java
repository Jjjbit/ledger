package com.ledger.ledger.business;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

//TODO
@RestController
@RequestMapping("/transactions")
public class TransactionController {
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LedgerRepository ledgerRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private LedgerCategoryComponentRepository ledgerCategoryComponentRepository;

    @DeleteMapping("{id}/delete")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deleteTransaction (@PathVariable Long id,
                                                     Principal principal) {
        //TODO testare
        User owner=userRepository.findByUsername(principal.getName());
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        Ledger ledger = transaction.getLedger();
        Account fromAccount = transaction.getFromAccount();
        Account toAccount = transaction.getToAccount();
        LedgerCategoryComponent categoryComponent = transaction.getCategory();

        if(ledger != null){
            ledger.getTransactions().remove(transaction);
            transaction.setLedger(null);
            ledgerRepository.save(ledger);
        }
        if(fromAccount != null){
            fromAccount.getOutgoingTransactions().remove(transaction);
            fromAccount.credit(transaction.getAmount());
            transaction.setFromAccount(null);
            accountRepository.save(fromAccount);
        }
        if(toAccount != null){
            toAccount.getIncomingTransactions().remove(transaction);
            toAccount.debit(transaction.getAmount());
            transaction.setToAccount(null);
            accountRepository.save(toAccount);
        }
        if(categoryComponent != null){
            categoryComponent.getTransactions().remove(transaction);
            transaction.setCategory(null);
            ledgerCategoryComponentRepository.save(categoryComponent);
        }

        transactionRepository.delete(transaction);
        return ResponseEntity.ok("Transaction deleted successfully");
    }
}

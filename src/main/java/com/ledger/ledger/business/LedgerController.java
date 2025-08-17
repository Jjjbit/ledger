package com.ledger.ledger.business;

import com.ledger.ledger.domain.Ledger;
import com.ledger.ledger.repository.LedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/ledgers")
public class LedgerController {
    @Autowired
    private LedgerRepository ledgerRepository;

    @PostMapping("/create-ledger")
    public void createLedger(@RequestParam Ledger ledger) {
        ledgerRepository.save(ledger);
    }

    @DeleteMapping("/ledgers/{ledgerId}/transactions/{transactionId}")
    public void deleteTransactionFromLedger(@PathVariable Long ledgerId, @PathVariable Long transactionId) {
        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new IllegalArgumentException("Ledger not found"));

    }

    //@PostMapping("/ledgers/{ledgerId}/transactions")
}

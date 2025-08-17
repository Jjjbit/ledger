package com.ledger.ledger.business;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.AccountRepository;
import com.ledger.ledger.repository.InstallmentPlanRepository;
import com.ledger.ledger.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import java.math.BigDecimal;
import java.time.MonthDay;
import java.util.List;

@RestController
@RequestMapping("/credit-accounts")
public class CreditAccountController {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create-credit-account")
    @Transactional
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
        User owner = userRepository.findByUsername(principal.getName());
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        CreditAccount account = new CreditAccount(
                accountName,
                balance,
                owner,
                note,
                includedInNetWorth,
                selectable,
                creditLimit,
                currentDebt,
                billDate,
                dueDate,
                type);
        accountRepository.save(account);
        userRepository.save(owner);
        return ResponseEntity.ok("Credit account created successfully");
    }

    @PutMapping("{id}/repay-debt")
    @Transactional
    public ResponseEntity<String> repayDebt(@PathVariable Long id,
                                            @RequestParam BigDecimal amount,
                                            @RequestParam(required = false) Long fromAccountId) {
        CreditAccount creditAccount = (CreditAccount) accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found"));
        Account fromAccount = (fromAccountId != null) ? accountRepository.findById(fromAccountId)
                .orElse(null) : null;

        creditAccount.repayDebt(amount, fromAccount);
        accountRepository.save(creditAccount);
        if (fromAccount != null) {
            accountRepository.save(fromAccount);
        }
        userRepository.save(creditAccount.getOwner());
        return ResponseEntity.ok("Debt repaid successfully");
    }

    @PutMapping("{id}/repay-installment-plan")
    @Transactional
    public ResponseEntity<String> repayInstallmentPlan(@PathVariable Long id,
                                                       @RequestParam Long installmentPlanId) {
        CreditAccount account = (CreditAccount) accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        InstallmentPlan installmentPlan = installmentPlanRepository.findById(installmentPlanId)
                .orElseThrow(() -> new IllegalArgumentException("Installment plan not found"));

        account.repayInstallmentPlan(installmentPlan);
        accountRepository.save(account);
        userRepository.save(account.getOwner());
        installmentPlanRepository.save(installmentPlan);
        return ResponseEntity.ok("Installment plan repaid successfully");
    }

    @GetMapping("{id}/get-installment-plans")
    public ResponseEntity<List<InstallmentPlan>> getInstallmentPlans(@PathVariable Long id) {
        CreditAccount account = (CreditAccount) accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        List<InstallmentPlan> installmentPlans = account.getInstallmentPlans();
        return ResponseEntity.ok(installmentPlans);
    }

    @PostMapping("{id}/add-installment-plan")
    public BigDecimal getCurrentDebt(@PathVariable Long id) {
        CreditAccount account = (CreditAccount) accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return account.getCurrentDebt();
    }
}

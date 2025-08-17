package com.ledger.ledger.business;

import com.ledger.ledger.domain.Account;
import com.ledger.ledger.domain.LoanAccount;
import com.ledger.ledger.domain.User;
import com.ledger.ledger.repository.AccountRepository;
import com.ledger.ledger.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.MonthDay;

@RestController
@RequestMapping("/loan-accounts")
public class LoanAccountController {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create-loan-account")
    @Transactional
    public ResponseEntity<String> createLoanAccount(@RequestParam String name,
                                                    Principal principal,
                                                    @RequestParam String notes,
                                                    @RequestParam boolean includedInNetWorth,
                                                    @RequestParam int totalPeriods,
                                                    @RequestParam int repaidPeriods,
                                                    @RequestParam BigDecimal interestRate,
                                                    @RequestParam BigDecimal loanAmount,
                                                    @RequestParam Account receivingAccount,
                                                    @RequestParam MonthDay repaymentDate,
                                                    @RequestParam LoanAccount.RepaymentType repaymentType) {
        User owner = userRepository.findByUsername(principal.getName());
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        LoanAccount loanAccount = new LoanAccount(name, owner, notes, includedInNetWorth, totalPeriods, repaidPeriods, interestRate, loanAmount, receivingAccount, repaymentDate, repaymentType);
        accountRepository.save(loanAccount);
        userRepository.save(owner);
        return ResponseEntity.ok("Loan account created successfully");
    }

    @PutMapping("{id}/repay-loan")
    @Transactional
    public ResponseEntity<String> repayLoan(@PathVariable Long id,
                                            @RequestParam(required = false) Long fromAccountId,
                                            Principal principal) {
        LoanAccount loanAccount = (LoanAccount) accountRepository.findById(id).orElse(null);
        if (loanAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Loan account not found");
        }
        User owner = userRepository.findByUsername(principal.getName());
        if (owner == null || !loanAccount.getOwner().equals(owner)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }
        Account fromAccount = (fromAccountId != null) ? accountRepository.findById(fromAccountId).orElse(null) : null;
        loanAccount.repayLoan(fromAccount);
        accountRepository.save(loanAccount);
        if (fromAccount != null) {
            accountRepository.save(fromAccount);
        }
        userRepository.save(loanAccount.getOwner());
        return ResponseEntity.ok("Loan repaid successfully");
    }

}

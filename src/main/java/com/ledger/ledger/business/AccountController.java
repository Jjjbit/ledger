package com.ledger.ledger.business;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.*;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

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
    private InstallmentPlanRepository installmentPlanRepository;
    @Autowired
    private LedgerRepository ledgerRepository;

    @PostMapping("/create-basic-account")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> createBasicAccount(@RequestParam String accountName,
                                                     @RequestParam BigDecimal balance,
                                                     Principal principal,
                                                     @RequestParam (required = false) String note,
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
                                                      @RequestParam (required = false) String note,
                                                      @RequestParam boolean includedInNetWorth,
                                                      @RequestParam boolean selectable,
                                                      @RequestParam BigDecimal creditLimit,
                                                      @RequestParam (required = false) BigDecimal currentDebt,
                                                      @RequestParam (required = false) Integer billDate,
                                                      @RequestParam (required = false) Integer dueDate,
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
                                                    @RequestParam (required = false) String note,
                                                    @RequestParam boolean includedInNetAsset,
                                                    Principal principal,
                                                    @RequestParam int totalPeriods,
                                                    @RequestParam int repaidPeriods,
                                                    @RequestParam BigDecimal annualInterestRate,
                                                    @RequestParam BigDecimal loanAmount,
                                                    @RequestParam (required = false) Long receivingAccountId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate repaymentDate,
                                                    @RequestParam (required = false) LoanAccount.RepaymentType repaymentType) {
        User owner = userRepository.findByUsername(principal.getName());
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Account receivingAccount=null;
        if(receivingAccountId != null) {
            receivingAccount = accountRepository.findById(receivingAccountId).orElse(null);
        }

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
        return ResponseEntity.ok("Loan account created successfully");
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

        if (deleteTransactions) {// Delete all transactions associated with the account
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
            return ResponseEntity.ok("Account and associated transactions deleted successfully");
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
                                              @RequestParam (required = false) String name,
                                              @RequestParam (required = false) BigDecimal balance,
                                              @RequestParam (required = false) String notes,
                                              @RequestParam (required = false) Boolean includedInNetAsset,
                                              @RequestParam (required = false) Boolean selectable) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!account.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot edit someone else's account");
        }

        if (!(account instanceof BasicAccount)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Account is not a basic account");
        }

        BasicAccount basicAccount = (BasicAccount) account;
        if (name!=null) {
            basicAccount.setName(name);
        }
        if(balance !=null){
            basicAccount.setBalance(balance);
        }
        basicAccount.setNotes(notes);

        if(includedInNetAsset != null){
            basicAccount.setIncludedInNetAsset(includedInNetAsset);
        }

        if(selectable !=null){
            basicAccount.setSelectable(selectable);
        }

        accountRepository.save(basicAccount);
        userRepository.save(basicAccount.getOwner());
        return ResponseEntity.ok("Account edited successfully");
    }

    @PutMapping("/{id}/edit-credit-account")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> editCreditAccount(@PathVariable Long id,
                                              Principal principal,
                                              @RequestParam (required = false) String name,
                                              @RequestParam (required = false) BigDecimal balance,
                                              @RequestParam (required = false) String notes,
                                              @RequestParam (required = false) Boolean includedInNetAsset,
                                              @RequestParam (required = false) Boolean selectable,
                                              @RequestParam (required = false) BigDecimal creditLimit,
                                              @RequestParam (required = false) BigDecimal currentDebt,
                                              @RequestParam (required = false) Integer billDate,
                                              @RequestParam (required = false) Integer dueDate) {
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

        if(name != null){
            creditAccount.setName(name);
        }
        if(balance != null){
            creditAccount.setBalance(balance);
        }
        creditAccount.setNotes(notes);
        if(includedInNetAsset != null){
            creditAccount.setIncludedInNetAsset(includedInNetAsset);
        }
        if(selectable != null){
            creditAccount.setSelectable(selectable);
        }
        if(creditLimit != null){
            creditAccount.setCreditLimit(creditLimit);
        }
        if(currentDebt != null){
            creditAccount.setCurrentDebt(currentDebt);
        }
        if(billDate != null){
            creditAccount.setBillDay(billDate);
        }
        if (dueDate != null){
            creditAccount.setDueDay(dueDate);
        }
        accountRepository.save(creditAccount);
        userRepository.save(creditAccount.getOwner());
        return ResponseEntity.ok("Credit account edited successfully");
    }

    @PutMapping("/{id}/edit-loan-account")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> editLoanAccount(@PathVariable Long id,
                                                  Principal principal,
                                                  @RequestParam (required = false) String name,
                                                  @RequestParam (required = false) String notes,
                                                  @RequestParam (required = false) Boolean includedInNetAsset,
                                                  @RequestParam (required = false) Integer totalPeriods,
                                                  @RequestParam (required = false) Integer repaidPeriods,
                                                  @RequestParam (required = false) BigDecimal annualInterestRate,
                                                  @RequestParam (required = false) BigDecimal loanAmount,
                                                  @RequestParam (required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate repaymentDate,
                                                  @RequestParam (required = false) LoanAccount.RepaymentType repaymentType) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!account.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot edit someone else's account");
        }
        if (!(account instanceof LoanAccount)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Account is not a loan account");
        }
        LoanAccount loanAccount = (LoanAccount) account;
        if(name != null){loanAccount.setName(name);}
        loanAccount.setNotes(notes);
        if(includedInNetAsset != null){loanAccount.setIncludedInNetAsset(includedInNetAsset);}
        if(totalPeriods != null){loanAccount.setTotalPeriods(totalPeriods);}
        if(repaidPeriods != null){
            loanAccount.setRepaidPeriods(repaidPeriods);
        }
        if(annualInterestRate != null){loanAccount.setAnnualInterestRate(annualInterestRate);}
        if(loanAmount != null){loanAccount.setLoanAmount(loanAmount);}
        if(repaymentDate != null) {loanAccount.setRepaymentDate(repaymentDate);}
        if(repaymentType != null){loanAccount.setRepaymentType(repaymentType);}
        ((LoanAccount) account).updateRemainingAmount();
        accountRepository.save(loanAccount);
        userRepository.save(loanAccount.getOwner());
        return ResponseEntity.ok("Loan account edited successfully");
    }


    @PutMapping("/{id}/credit")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> creditAccount(@PathVariable Long id,
                                                @RequestParam BigDecimal amount,
                                                Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!account.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot credit someone else's account");
        }

        if(account instanceof LoanAccount){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot credit a loan account");
        }

        account.credit(amount);
        accountRepository.save(account);
        return ResponseEntity.ok("credit account");
    }

    @PutMapping("/{id}/debit")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> debitAccount(@PathVariable Long id,
                                               @RequestParam BigDecimal amount,
                                               Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!account.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot debit someone else's account");
        }

        if(account instanceof LoanAccount){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot debit a loan account");
        }
        account.debit(amount);
        accountRepository.save(account);
        return ResponseEntity.ok("debit account");
    }

    @PutMapping("{id}/repay-debt")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> repayDebt(@PathVariable Long id,
                                            @RequestParam BigDecimal amount,
                                            @RequestParam(required = false) Long fromAccountId,
                                            Principal principal,
                                            @RequestParam (required = false) Long ledgerId) {

        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        CreditAccount creditAccount = (CreditAccount) accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found"));

        Account fromAccount = (fromAccountId != null) ? accountRepository.findById(fromAccountId)
                .orElse(null) : null;

        if (!creditAccount.getOwner().getId().equals(user.getId()) || !fromAccount.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot repay debt");
        }

        Ledger ledger=null;
        if(ledgerId != null) {
            ledger=ledgerRepository.findById(ledgerId).orElse(null);
        }

        creditAccount.repayDebt(amount, fromAccount, ledger);

        accountRepository.save(creditAccount);
        if (fromAccount != null) {
            accountRepository.save(fromAccount);
        }
        if(ledger != null){
            ledgerRepository.save(ledger);
        }
        return ResponseEntity.ok("Debt repaid successfully");
    }

    @PutMapping("{id}/repay-installment-plan")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> repayInstallmentPlan(@PathVariable Long id,
                                                       @RequestParam Long installmentPlanId,
                                                       Principal principal,
                                                       @RequestParam (required = false) BigDecimal amount,
                                                       @RequestParam (required = false) Long ledgerId) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        CreditAccount account = (CreditAccount) accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        InstallmentPlan installmentPlan = installmentPlanRepository.findById(installmentPlanId)
                .orElseThrow(() -> new IllegalArgumentException("Installment plan not found"));

        if (!account.getOwner().equals(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You cannot repay someone else's installment plan");
        }

        if (!account.getInstallmentPlans().contains(installmentPlan)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Installment plan does not belong to this account");
        }
        Ledger ledger=null;
        if(ledgerId != null) {
           ledger= ledgerRepository.findById(ledgerId).orElse(null);
        }

        if(amount != null){
            account.repayInstallmentPlan(installmentPlan, amount, ledger);
        }else {
            account.repayInstallmentPlan(installmentPlan, ledger);
        }
        if(ledger != null){
            ledgerRepository.save(ledger);
        }

        accountRepository.save(account);
        return ResponseEntity.ok("Installment plan repaid successfully");
    }

    //LoanAccount
    @PutMapping("{id}/repay-loan")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> repayLoan(@PathVariable Long id,
                                            @RequestParam(required = false) Long fromAccountId,
                                            Principal principal,
                                            @RequestParam (required = false) BigDecimal amount,
                                            @RequestParam (required = false) Long ledgerId) {
        LoanAccount loanAccount = (LoanAccount) accountRepository.findById(id).orElse(null);
        if (loanAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Loan account not found");
        }

        Account fromAccount = (fromAccountId != null) ? accountRepository.findById(fromAccountId).orElse(null) : null;

        User owner = userRepository.findByUsername(principal.getName());

        if (owner == null ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }

        Ledger ledger=null;
        if(ledgerId != null) {
            ledger=ledgerRepository.findById(ledgerId).orElse(null);
        }

        if(amount != null){
            loanAccount.repayLoan(fromAccount, amount, ledger);
        }else{
            loanAccount.repayLoan(fromAccount, ledger);
        }

        accountRepository.save(loanAccount);
        if (fromAccount != null) {
            accountRepository.save(fromAccount);
        }
        if(ledger != null){
            ledgerRepository.save(ledger);
        }
        return ResponseEntity.ok("Loan repaid successfully");
    }



    @GetMapping("{id}/get-transacitons-for-month")
    public List<Transaction> getAccountTransactionsForMonth(@PathVariable Long id,
                                                            @RequestParam YearMonth month) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return account.getTransactionsForMonth(month);
    }
}

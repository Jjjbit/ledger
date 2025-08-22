package com.ledger.ledger.business;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.AccountRepository;
import com.ledger.ledger.repository.InstallmentPlanRepository;
import com.ledger.ledger.repository.TransactionRepository;
import com.ledger.ledger.repository.UserRepository;

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
    private InstallmentPlanRepository installmentPlanRepository;

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

        Account receivingAccount =null;
        if(receivingAccountId != null) {
             receivingAccount = accountRepository.findById(receivingAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));
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
        if (deleteTransactions) {// Delete all transactions associated with the account
            List<Transaction> transactionsToDelete = new ArrayList<>(account.getTransactions());
            for (Transaction transaction : transactionsToDelete) {
                transaction.getAccount().getTransactions().remove(transaction);
                transaction.setAccount(null);

                transaction.getLedger().getTransactions().remove(transaction);
                transaction.setLedger(null);

                transaction.getCategory().getTransactions().remove(transaction);
                transaction.setCategory(null);

                transactionRepository.save(transaction);
                transactionRepository.delete(transaction);

                if(account instanceof BasicAccount) {
                    user.setTotalAssets(
                            user.calculateTotalAssets().subtract(account.getBalance())
                    );
                    user.setNetAssets(user.getTotalAssets().subtract(user.getTotalLiabilities()));
                }else if(account instanceof CreditAccount){
                    user.setTotalAssets(
                            user.calculateTotalAssets().subtract(account.getBalance())
                    );
                    user.setTotalLiabilities(
                            user.calculateTotalLiabilities().subtract(((CreditAccount) account).getCurrentDebt())
                    );
                    user.setNetAssets(user.getTotalAssets().subtract(user.getTotalLiabilities()));
                }else{
                    user.setTotalLiabilities(
                            user.calculateTotalLiabilities().subtract(((LoanAccount) account).getRemainingAmount())
                    );
                    user.setNetAssets(user.getTotalAssets().subtract(user.getTotalLiabilities()));
                }
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
        if(name!=null) {
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
        basicAccount.getOwner().updateTotalAssets();
        basicAccount.getOwner().updateNetAsset();
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
        creditAccount.getOwner().updateTotalAssets();
        creditAccount.getOwner().updateTotalLiabilities();
        creditAccount.getOwner().updateNetAsset();
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
        if(repaidPeriods != null){loanAccount.setRepaidPeriods(repaidPeriods);}
        if(annualInterestRate != null){loanAccount.setAnnualInterestRate(annualInterestRate);}
        if(loanAmount != null){loanAccount.setLoanAmount(loanAmount);}
        if(repaymentDate != null) {loanAccount.setRepaymentDate(repaymentDate);}
        if(repaymentType != null){loanAccount.setRepaymentType(repaymentType);}
        accountRepository.save(loanAccount);
        loanAccount.getOwner().updateTotalAssets();
        loanAccount.getOwner().updateTotalLiabilities();
        loanAccount.getOwner().updateNetAsset();
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
        account.debit(amount);
        accountRepository.save(account);
        return ResponseEntity.ok("debit account");
    }

    //CreditAccount
    @PutMapping("{id}/repay-debt")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> repayDebt(@PathVariable Long id,
                                            @RequestParam BigDecimal amount,
                                            @RequestParam(required = false) Long fromAccountId,
                                            Principal principal) {

        CreditAccount creditAccount = (CreditAccount) accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found"));
        Account fromAccount = (fromAccountId != null) ? accountRepository.findById(fromAccountId)
                .orElse(null) : null;

        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        if (!creditAccount.getOwner().getId().equals(user.getId()) || !fromAccount.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot repay debt");
        }

        creditAccount.repayDebt(amount, fromAccount);
        accountRepository.save(creditAccount);
        if (fromAccount != null) {
            accountRepository.save(fromAccount);
        }
        return ResponseEntity.ok("Debt repaid successfully");
    }

    @PutMapping("{id}/repay-installment-plan")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> repayInstallmentPlan(@PathVariable Long id,
                                                       @RequestParam Long installmentPlanId, Principal principal) {

        CreditAccount account = (CreditAccount) accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        InstallmentPlan installmentPlan = installmentPlanRepository.findById(installmentPlanId)
                .orElseThrow(() -> new IllegalArgumentException("Installment plan not found"));

        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        if (!account.getOwner().getId().equals(user.getId()) || !account.getInstallmentPlans().contains(installmentPlan)) {
            throw new AccessDeniedException("You cannot repay installment");
        }

        account.repayInstallmentPlan(installmentPlan);
        accountRepository.save(account);
        installmentPlanRepository.save(installmentPlan);
        return ResponseEntity.ok("Installment plan repaid successfully");
    }

    //LoanAccount
    @PutMapping("{id}/repay-loan")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> repayLoan(@PathVariable Long id,
                                            @RequestParam(required = false) Long fromAccountId,
                                            Principal principal) {
        LoanAccount loanAccount = (LoanAccount) accountRepository.findById(id).orElse(null);
        if (loanAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Loan account not found");
        }

        Account fromAccount = (fromAccountId != null) ? accountRepository.findById(fromAccountId).orElse(null) : null;

        User owner = userRepository.findByUsername(principal.getName());
        if (owner == null || !loanAccount.getOwner().equals(owner) || !fromAccount.getOwner().equals(owner)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }

        loanAccount.repayLoan(fromAccount);
        accountRepository.save(loanAccount);
        if (fromAccount != null) {
            accountRepository.save(fromAccount);
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

package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "owner",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ledger> ledgers= new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Account> accounts= new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Budget> budgets= new ArrayList<>();

    @Column(name = "total_assets", precision = 15, scale = 2, nullable = true)
    private BigDecimal totalAssets;

    @Column(name = "total_liabilities", precision = 15, scale = 2, nullable = true)
    private BigDecimal totalLiabilities;

    @Column(name = "net_assets", precision = 15, scale = 2, nullable = true)
    private BigDecimal netAssets;

    public User (){}
    public User(String username,
                String password
                ) {
        this.username = username;
        this.password = password;
        Ledger ledger = new Ledger("Default Ledger", this);
        ledgers.add(ledger);
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
    public void createLedger(String name) {
        Ledger ledger = new Ledger(name, this);
        ledgers.add(ledger);
    }
    public List<Ledger> getLedgers() {
        return ledgers;
    }
    public List<Account> getAccounts() {
        return accounts;
    }
    public List<Budget> getBudgets() {
        return budgets;
    }

    public String getPassword(){return password;}
    public String getUsername(){return username;}
    public void setBudget(BigDecimal amount, Budget.Period p, LedgerCategoryComponent c) {
        budgets.add(new Budget(amount, p, c,this));
    }
    public void deleteAccount(Account account, boolean deleteTransactions) {

        if (deleteTransactions) {// Delete all transactions associated with the account
            List<Transaction> transactionsToDelete = new ArrayList<>(account.getTransactions());
            for (Transaction transaction : transactionsToDelete) {
                if (transaction.getFromAccount() != null) {
                    transaction.getFromAccount().getTransactions().remove(transaction);
                    transaction.setFromAccount(null);
                }

                if (transaction instanceof Transfer) {
                    Transfer transfer = (Transfer) transaction;
                    Account toAccount = transfer.getToAccount();
                    if (toAccount != null) {
                        toAccount.getTransactions().remove(transfer);
                        transfer.setToAccount(null);
                    }
                }

                if (transaction.getLedger() != null) {
                    transaction.getLedger().getTransactions().remove(transaction);
                    transaction.setLedger(null);
                }

                if (transaction.getCategory() != null){
                    transaction.getCategory().getTransactions().remove(transaction);
                    transaction.setCategory(null);
                }
            }
            account.getTransactions().clear();
        } else {
            for (Transaction transaction : account.getTransactions()) {

                if (transaction.getFromAccount() != null) {
                    transaction.getFromAccount().getTransactions().remove(transaction);
                    transaction.setFromAccount(null);
                }

                if (transaction instanceof Transfer) {
                    Transfer transfer = (Transfer) transaction;
                    Account toAccount = transfer.getToAccount();
                    if (toAccount != null) {
                        toAccount.getTransactions().remove(transfer);
                        transfer.setToAccount(null);
                    }

                }

            }
        }
        account.setOwner(null);
        accounts.remove(account);
    }

    public void addAccount(Account account) {
        accounts.add(account);
        account.setOwner(this);
        this.totalAssets = getTotalAssets();
        this.totalLiabilities = getTotalLiabilities();
        this.netAssets = getNetAssets();
    }

    public BigDecimal getTotalLending(){
        return accounts.stream()
                .filter(account -> account instanceof LendingAccount)
                .filter(account -> account.includedInNetAsset && !account.hidden)
                .filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalBorrowing() {
        return accounts.stream()
                .filter(account -> account instanceof BorrowingAccount)
                .filter(account -> account.includedInNetAsset && !account.hidden)
                .filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalAssets() {
        BigDecimal totalBalance = accounts.stream()
                .filter(account -> !account.getType().equals(AccountType.LOAN))
                .filter(account -> account.includedInNetAsset && !account.hidden)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalBalance.add(getTotalLending());
    }

    public BigDecimal getNetAssets() {
        return getTotalAssets().subtract(getTotalLiabilities()).add(getTotalLending());
    }
    public BigDecimal getTotalLiabilities() {
        BigDecimal totalCreditDebt = accounts.stream()
                .filter(account -> account.getCategory() == AccountCategory.CREDIT)
                .filter(account -> account instanceof CreditAccount)
                .filter(account-> account.includedInNetAsset && !account.hidden)
                .map(account -> ((CreditAccount) account).getCurrentDebt())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUnpaidLoan = accounts.stream()
                .filter(account -> account.getCategory() == AccountCategory.CREDIT)
                .filter(account -> account instanceof LoanAccount)
                .filter(account -> account.includedInNetAsset && !account.hidden)
                .filter(account -> !((LoanAccount) account).isEnded)
                .map(account -> ((LoanAccount) account).getRemainingAmount()) //get this.remainingAmount
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalCreditDebt.add(getTotalBorrowing()).add(totalUnpaidLoan);
    }

}




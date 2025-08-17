package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @OneToMany(mappedBy = "owner",cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Ledger> ledgers;

    @OneToMany(mappedBy = "owner", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Account> accounts;

    @OneToMany(mappedBy = "owner", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    public List<Budget> budgets= new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    public List<BorrowingAndLending> bAndL;

    @Column(name = "total_assets", precision = 15, scale = 2, nullable = true)
    public BigDecimal totalAssets;

    @Column(name = "total_liabilities", precision = 15, scale = 2, nullable = true)
    public BigDecimal totalLiabilities;

    @Column(name = "net_assets", precision = 15, scale = 2, nullable = true)
    public BigDecimal netAssets;

    public User (){}
    public User(String username, String password){
        this.username = username;
        this.password = password;
        ledgers= new ArrayList<>();
        createLedger("Default Ledger");
        accounts = new ArrayList<>();
        bAndL = new ArrayList<>();
        this.totalAssets = getTotalAssets();
        this.totalLiabilities = getTotalLiabilities();
        this.netAssets = getNetAssets();
    }

    public String getUsername() {
        return username;
    }
    public String getPasswordHash(){return password;}
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
    public void setBudget(BigDecimal amount, Budget.Period p, CategoryComponent c) {
        budgets.add(new Budget(amount, p, c,this));
    }
    public void deleteAccount(Account account) {
        accounts.remove(account);
        this.totalAssets = getTotalAssets();
        this.totalLiabilities = getTotalLiabilities();
        this.netAssets = getNetAssets();

    }

    public void addAccount(Account account) {
        accounts.add(account);
        account.setOwner(this);
        this.totalAssets = getTotalAssets();
        this.totalLiabilities = getTotalLiabilities();
        this.netAssets = getNetAssets();
    }
    public void addBorrowingAndLending(BorrowingAndLending record) {
        bAndL.add(record);
    }

    public BigDecimal getTotalLending(){
        return bAndL.stream()
                .filter(record -> !record.isIncoming())
                .filter(record -> record.includedInNetWorth)
                .filter(record -> !record.isEnded)
                .map(BorrowingAndLending::getRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalBorrowing() {
        return bAndL.stream()
                .filter(BorrowingAndLending::isIncoming)
                .filter(record -> record.includedInNetWorth)
                .filter(record -> !record.isEnded)
                .map(BorrowingAndLending::getRemaining)
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
        return getTotalAssets().subtract(getTotalLiabilities());
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
                .filter(account -> account.includedInNetAsset)
                .map(account -> ((LoanAccount) account).getRemainingAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalCreditDebt.add(getTotalBorrowing()).add(totalUnpaidLoan);
    }
    public void updateNetAssetsAndLiabilities(BigDecimal amount) {
        this.totalLiabilities = getTotalLiabilities().subtract(amount);
        //this.netAssets = this.netAssets.add(amount);
        this.netAssets = getTotalAssets().subtract(this.totalLiabilities);
    }
    public void updateNetAsset(){
        this.netAssets= getNetAssets();
    }
    public void updateTotalAssets(){
        this.totalAssets = getTotalAssets();
    }
    public void updateTotalLiabilities(){
        this.totalLiabilities = getTotalLiabilities();
    }
}




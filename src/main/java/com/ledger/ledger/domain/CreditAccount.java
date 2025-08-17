package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credit_account")
public class CreditAccount extends Account {

    @Column(name = "credit_limit", precision = 15, scale = 2, nullable = true)
    private BigDecimal creditLimit= BigDecimal.ZERO;

    @Column(name = "current_debt", precision = 15, scale = 2, nullable = true)
    private BigDecimal currentDebt = BigDecimal.ZERO;

    @Column(name = "bill_date")
    private MonthDay billDate;

    @Column(name = "due_date")
    private MonthDay dueDate;

    @OneToMany(mappedBy = "linkedAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InstallmentPlan> installmentPlans;

    public CreditAccount(){
    }
    public CreditAccount(String name,
                         BigDecimal balance,
                         User owner,
                         String notes,
                         boolean includedInNetWorth,
                         boolean selectable,
                         BigDecimal creditLimit,
                         BigDecimal currentDebt, //contiene importo residuo delle rate
                         MonthDay billDate,
                         MonthDay dueDate, AccountType type) {
        super(name, balance, type, AccountCategory.CREDIT, owner, notes, includedInNetWorth, selectable);
        this.creditLimit = creditLimit;
        this.currentDebt = currentDebt;
        this.billDate = billDate;
        this.dueDate = dueDate;
        this.installmentPlans = new ArrayList <>();
        this.owner.addAccount(this);
    }

    public BigDecimal getCurrentDebt() {
        return currentDebt;
    }

    @Override
    public void debit(BigDecimal amount) {
        if (amount.compareTo(balance) > 0) { //amount>balance
            if (currentDebt.add(amount.subtract(balance)).compareTo(creditLimit) > 0) { //currentDebt+(amount-balance)>creditLimit
                throw new IllegalArgumentException("Amount exceeds credit limit");
            }else{
                balance = BigDecimal.ZERO;
                //addNewDebt(amount.subtract(balance));
                currentDebt = currentDebt.add(amount.subtract(balance));
            }
        }else{
            balance = balance.subtract(amount);
        }
        this.owner.updateTotalLiabilities();
        this.owner.updateTotalAssets();
        this.owner.updateNetAsset();
    }
    public void addNewDebt(BigDecimal amount) {
        currentDebt = currentDebt.add(amount);
        //this.owner.updateNetAssetsAndLiabilities(amount);
    }

    public void repayDebt(BigDecimal amount, Account fromAccount) {
        currentDebt = currentDebt.subtract(amount);
        if(fromAccount != null) {
            fromAccount.debit(amount);
        }else {
            this.owner.updateNetAssetsAndLiabilities(amount);
        }
        this.owner.updateTotalAssets();
        this.owner.updateTotalLiabilities();
        this.owner.updateNetAsset();
    }

    public List<InstallmentPlan> getInstallmentPlans() {
        return installmentPlans;
    }
    public void addInstallmentPlan(InstallmentPlan installmentPlan) {
        installmentPlans.add(installmentPlan);
        currentDebt = currentDebt.add(installmentPlan.getRemainingAmount());
    }
    public void removeInstallmentPlan(InstallmentPlan installmentPlan) {
        installmentPlans.remove(installmentPlan);
        currentDebt = currentDebt.subtract(installmentPlan.getRemainingAmount());
    }
    public void repayInstallmentPlan(InstallmentPlan installmentPlan) {
        if (installmentPlans.contains(installmentPlan)) {
            BigDecimal amount = installmentPlan.getMonthlyPayment(installmentPlan.getPaidPeriods() + 1);
            installmentPlan.repayOnePeriod();
            currentDebt = currentDebt.subtract(amount);
            this.owner.updateNetAssetsAndLiabilities(amount);
        } else {
            throw new IllegalArgumentException("Installment plan not found in this account");
        }
    }

    //ritorna il totale delle rate ancora da pagare collegate a questo account
    public BigDecimal getRemainingInstallmentDebt(){
        return installmentPlans.stream()
                .map(InstallmentPlan::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}



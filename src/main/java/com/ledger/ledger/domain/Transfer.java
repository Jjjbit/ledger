package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "transfer")
public class Transfer extends Transaction{

    @ManyToOne
    @JoinColumn(name = "toAccount_id")
    private Account toAccount;

    public Transfer() {}
    public Transfer(LocalDate date,
                    String description,
                    Account from,
                    Account to,
                    BigDecimal amount,
                    Ledger ledger) {
        super(date, amount, description, from, ledger, null, TransactionType.TRANSFER);
        this.toAccount = to;
    }

    public Account getToAccount() {
        return toAccount;
    }
    public void setToAccount(Account toAccount) {
        this.toAccount = toAccount;
    }

    @Override
    public void execute() {
        if (account == null && toAccount == null) {
            throw new IllegalArgumentException("select account");
        }

        if(account !=null) {
            account.debit(amount);
        }
        if (toAccount != null) {
            if (toAccount.getType().equals(AccountType.LOAN)) {//for repayLoan
                ((LoanAccount) toAccount).setRemainingAmount( ((LoanAccount) toAccount).calculateRemainingLoanAmount().subtract(amount));
            }else if(toAccount.getType().equals(AccountType.CREDIT_CARD)){ //for repayInstallPlan
                ((CreditAccount) toAccount).setCurrentDebt(((CreditAccount) toAccount).getCurrentDebt().subtract(amount));
            } else {
                toAccount.credit(amount);
            }
        }
        if( account!= null) {
            account.getOwner().updateTotalAssets();
            account.getOwner().updateTotalLiabilities();
            account.getOwner().updateNetAsset();
        }else if(toAccount != null){
            toAccount.getOwner().updateTotalAssets();
            toAccount.getOwner().updateTotalLiabilities();
            toAccount.getOwner().updateNetAsset();
        }
    }

    @Override
    public void rollback(){
        account.credit(amount);
        if(toAccount != null) {
            toAccount.debit(amount);
        }
        account.getOwner().updateTotalAssets();
        account.getOwner().updateTotalLiabilities();
        account.getOwner().updateNetAsset();
    }
}

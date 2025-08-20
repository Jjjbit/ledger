package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transfer")
public class Transfer extends Transaction{

    @ManyToOne
    @JoinColumn(name = "toAccount_id", nullable=false)
    private Account toAccount;

    public Transfer() {}
    public Transfer(LocalDate date,
                    String description,
                    Account from,
                    Account to,
                    BigDecimal amount,
                    Ledger ledger) {
        super(date, amount, description, from, ledger, null, TransactionType.TRANSFER);
        this.account = from;
        this.toAccount = to;
    }

    public Account getToAccount() {
        return toAccount;
    }
    public void setToAccount(Account toAccount) {
        this.toAccount = toAccount;
    }

    public void execute() {
        if (account.equals(toAccount)) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }
        if(account==null && toAccount==null){
            throw new IllegalArgumentException("select account");
        }else {
            if (!account.selectable || !toAccount.selectable || account.hidden || toAccount.hidden) {
                throw new IllegalStateException("Accounts are not valid for transfer.");
            }
        }

        account.debit(amount);
        if(toAccount !=null) {
            toAccount.credit(amount);
        }
        account.getOwner().updateTotalAssets();
        account.getOwner().updateTotalLiabilities();
        account.getOwner().updateNetAsset();
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

package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lending_account")
public class LendingAccount extends Account {

    public LendingAccount() {}
    public LendingAccount(String name,  //person or entity from whom the money is lent
                          BigDecimal balance, //bilancio da pagare da utente
                          String note,
                          boolean includedInNetWorth,
                          boolean selectable,
                          User owner) {
        super(name, balance, AccountType.LENDING, AccountCategory.VIRTUAL_ACCOUNT, owner, note, includedInNetWorth, selectable);
        if (this.owner != null) {
            this.owner.getAccounts().add(this);
        }
    }

    public void receiveRepayment(BigDecimal amount, Account toAccount, Ledger ledger) {
        toAccount.credit(amount);
        Transaction tx= new Transfer(
                LocalDate.now(),
                "Repayment from "+ name,
                this,
                toAccount,
                amount,
                ledger
        );
        this.balance = this.balance.subtract(amount);
        this.getOutgoingTransactions().add(tx);
        toAccount.getIncomingTransactions().add(tx);
        ledger.getTransactions().add(tx);
    }
    @Override
    public void credit(BigDecimal amount) {
        throw new UnsupportedOperationException("Credit operation is not supported for LendingAccount");
    }

    @Override
    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

}

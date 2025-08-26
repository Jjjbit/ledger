package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borrowing_account")
public class BorrowingAccount extends Account{

    public BorrowingAccount() {}
    public BorrowingAccount(String name,  //person or entity from whom the money is borrowed
                            BigDecimal balance, //bilancio da pagare da utente
                            String note,
                            boolean includedInNetWorth,
                            boolean selectable,
                            User owner) {
        super(name, balance, AccountType.BORROWING, AccountCategory.VIRTUAL_ACCOUNT, owner, note, includedInNetWorth, selectable);
        if (this.owner != null) {
            this.owner.getAccounts().add(this);
        }
    }
    @Override
    public void credit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    @Override
    public void debit(BigDecimal amount) {
        throw new UnsupportedOperationException("Credit operation is not supported for BorrowingAccount");
    }

    public void repay(BigDecimal amount, Account account, Ledger ledger) {
        account.debit(amount); //decrementa il balance dell'account
        Transaction tx = new Transfer(
                LocalDate.now(),
                "Repayment of borrowing to " + name,
                account,
                this,
                amount,
                ledger
        );
        balance = balance.subtract(amount); //decrementa il balance del borrowing
        this.getIncomingTransactions().add(tx); //aggiunge la transazione alla lista delle transazioni in entrata del borrowing
        account.getOutgoingTransactions().add(tx); //aggiunge la transazione alla lista delle transazioni in uscita dell'account
        ledger.getTransactions().add(tx); //aggiunge la transazione alla lista delle transazioni del ledger
        // controlla se il borrowing è stato completamente rimborsato
    }

}

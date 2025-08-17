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
        if (account == null || toAccount == null) {
            throw new IllegalStateException("Accounts must not be null.");
        }
        if (account.equals(toAccount)) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        if (!account.selectable || !toAccount.selectable || account.hidden || toAccount.hidden) {
            throw new IllegalStateException("Accounts are not valid for transfer.");
        }

        account.debit(amount);
        toAccount.credit(amount);
    }
}

package com.ledger.ledger.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expense")
public class Expense extends Transaction {

    public Expense() {}
    public Expense(LocalDate date,
                   BigDecimal amount,
                   String description,
                   Account account,
                   Ledger ledger,
                   CategoryComponent category) {
        super(date, amount, description, account, ledger, category, TransactionType.EXPENSE);
    }
    @Override
    public void execute() {
        if (!account.hidden && account.selectable){
            if (!account.getCategory().equals(AccountCategory.CREDIT) && account.balance.compareTo(amount) < 0) {
                throw new IllegalArgumentException("Insufficient funds in the account to execute this transaction.");
            }
            account.debit(amount);
        }

    }

}

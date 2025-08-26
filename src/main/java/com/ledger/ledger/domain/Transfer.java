package com.ledger.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
//@Table(name = "transfer")
@DiscriminatorValue("TRANSFER")
public class Transfer extends Transaction{

    public Transfer() {}
    public Transfer(LocalDate date,
                    String description,
                    Account from,
                    Account to,
                    BigDecimal amount,
                    Ledger ledger) {
        super(date, amount, description, from, to, ledger, null, TransactionType.TRANSFER);
    }

    @Override
    public void execute() {
        if (fromAccount != null) {
            fromAccount.debit(amount);
        }
        if (toAccount != null) {
            if (!(toAccount instanceof LoanAccount || toAccount instanceof CreditAccount)) {
                toAccount.credit(amount);
            }
        }
    }
    @Override
    public void rollback() {
        if (fromAccount != null) {
            fromAccount.credit(amount);
        }
        if (toAccount != null) {
            toAccount.debit(amount);
        }
    }
}

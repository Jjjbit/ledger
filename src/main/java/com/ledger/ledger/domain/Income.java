package com.ledger.ledger.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
//@Table(name = "income")
@DiscriminatorValue("INCOME")
public class Income extends Transaction {
    public  Income (LocalDate date,
                    BigDecimal amount,
                    String description,
                    Account account,
                    Ledger ledger,
                    LedgerCategoryComponent category) {
        super(date, amount, description, null, account, ledger, category, TransactionType.INCOME);
    }

    public Income() {}

    @Override
    public void execute() {
        if ( !toAccount.hidden && toAccount.selectable) {
            toAccount.credit(amount);
        }
    }
    @Override
    public void rollback(){
        toAccount.debit(amount);
    }

}

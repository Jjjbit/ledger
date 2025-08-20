package com.ledger.ledger.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "income")
public class Income extends Transaction {
    public  Income (LocalDate date, BigDecimal amount, String description, Account account, Ledger ledger, LedgerCategoryComponent category) {
        super(date, amount, description, account, ledger, category, TransactionType.INCOME);
    }

    public Income() {}

    @Override
    public void execute() {
        if (!account.hidden && account.selectable) {
            account.credit(amount);
        }
        account.getOwner().updateTotalAssets();
        account.getOwner().updateTotalLiabilities();
        account.getOwner().updateNetAsset();
    }

    @Override
    public void rollback(){
        account.debit(amount);
        account.getOwner().updateTotalAssets();
        account.getOwner().updateTotalLiabilities();
        account.getOwner().updateNetAsset();
    }

}

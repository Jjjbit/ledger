package com.ledger.ledger.domain;

import java.math.BigDecimal;
import java.time.MonthDay;
import java.util.Map;

public class AccountFactory {
    public static Account createAccount(AccountType type, AccountCategory category, Map<String, Object> fields) {
        if(category== AccountCategory.CREDIT) {
            return new CreditAccount(
                    (String) fields.get("name"),
                    (BigDecimal) fields.get("balance"),
                    (User) fields.get("owner"),
                    (String) fields.get("note"),
                    (Boolean) fields.get("includedInNetWorth"),
                    (Boolean) fields.get("selectable"),
                    (BigDecimal) fields.get("creditLimit"),
                    (BigDecimal) fields.get("currentDebt"),
                    (MonthDay) fields.get("billDate"),
                    (MonthDay) fields.get("dueDate"),
                    type
            );
        }else if(type == AccountType.LOAN) {
            return new LoanAccount(
                    (String) fields.get("name"),
                    (User) fields.get("owner"),
                    (String) fields.get("note"),
                    (Boolean) fields.get("includedInNetWorth"),
                    (Integer) fields.get("totalPeriods"),
                    (Integer) fields.get("repaidPeriods"),
                    (BigDecimal) fields.get("annualInterestRate"),
                    (BigDecimal) fields.get("loanAmount"),
                    (Account) fields.get("receivingAccount"),
                    (MonthDay) fields.get("repaymentDate"),
                    (LoanAccount.RepaymentType) fields.get("repaymentType")
            );
        } else {
            return new BasicAccount(
                    (String) fields.get("name"),
                    (BigDecimal) fields.get("balance"),
                    (String) fields.get("note"),
                    (Boolean) fields.get("includedInNetWorth"),
                    (Boolean) fields.get("selectable"),
                    type,
                    category,
                    (User) fields.get("owner")
            );
        }
    }
}

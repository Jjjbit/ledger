package com.ledger.ledger;

import com.ledger.ledger.domain.Category;
import com.ledger.ledger.domain.CategoryComponent;
import com.ledger.ledger.domain.SubCategory;
import com.ledger.ledger.domain.Transaction;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class LedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);

    }
}

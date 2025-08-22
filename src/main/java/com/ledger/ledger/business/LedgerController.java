package com.ledger.ledger.business;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.CategoryComponentRepository;
import com.ledger.ledger.repository.LedgerRepository;
import com.ledger.ledger.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/ledgers")
public class LedgerController {
    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryComponentRepository categoryComponentRepository;

    @PostMapping("/create-ledger")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> createLedger(@RequestParam String name,
                                               Principal principal) {
        User owner=userRepository.findByUsername(principal.getName());
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Ledger ledger=new Ledger(name, owner);
        ledgerRepository.save(ledger);

        //List<CategoryComponent> templateCategories = categoryComponentRepository.findAll();
        List<Category> rootCategories = categoryComponentRepository.findByParentIsNull();

        //copia albero di categorycomponent dal database a ledger
        for (Category template : rootCategories) {
            LedgerCategoryComponent ledgerCategoryComponent= copyCategoryComponent(template, ledger);
            ledger.addCategoryComponent(ledgerCategoryComponent);
        }
        return ResponseEntity.ok("ledger created successfully");
    }

    private LedgerCategoryComponent copyCategoryComponent(CategoryComponent template, Ledger ledger) {
        LedgerCategoryComponent copy;

        if (template instanceof Category) {

            LedgerCategory ledgerCategory = new LedgerCategory();
            ledgerCategory.setName(template.getName());
            ledgerCategory.setType(template.getType());
            ledgerCategory.setLedger(ledger);

            for (CategoryComponent templateSub : template.getChildren()) {
                LedgerCategoryComponent childCopy = copyCategoryComponent(templateSub, ledger);
                ledgerCategory.add(childCopy);

                //childCopy.setParent(ledgerCategory);
                //ledgerCategory.getChildren().add(childCopy);
            }
            copy = ledgerCategory; //returns category and its subcategories

        } else {

            LedgerSubCategory ledgerSub = new LedgerSubCategory();
            ledgerSub.setName(template.getName());
            ledgerSub.setLedger(ledger);
            ledgerSub.setType(template.getType());
            ledger.addCategoryComponent(ledgerSub);
            copy = ledgerSub; //returns subcategory
        }

        return copy;
    }

    @DeleteMapping("/{ledgerId}/delete")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deleteLedger(@PathVariable Long ledgerId,
                                               Principal principal){
        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new IllegalArgumentException("Ledger not found"));

        User owner=userRepository.findByUsername(principal.getName());
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        for(Transaction tx : ledger.getTransactions()){
            tx.rollback();
        }

        ledgerRepository.delete(ledger);
        return ResponseEntity.ok("delete ledger");
    }

    @PostMapping("/{ledgerId}/")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> copyLedger(@PathVariable Long ledgerId,
                                             Principal principal) {
        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new IllegalArgumentException("Ledger not found"));

        User owner=userRepository.findByUsername(principal.getName());
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Ledger newLedger=new Ledger(ledger.getName()+ " Copy", ledger.getOwner());

        for (LedgerCategoryComponent oldCategory : ledger.getCategoryComponents()) {
            LedgerCategoryComponent newCategory = copyLedgerCategory(oldCategory, newLedger);
            newLedger.addCategoryComponent(newCategory);
        }

        ledgerRepository.save(newLedger);
        return ResponseEntity.ok("copy ledger");
    }

    private LedgerCategoryComponent copyLedgerCategory(LedgerCategoryComponent oldCategory, Ledger newLedger) {
        LedgerCategoryComponent copy;

        if (oldCategory instanceof LedgerCategory) {
            LedgerCategory newCategory = new LedgerCategory();
            newCategory.setName(oldCategory.getName());
            newCategory.setLedger(newLedger);

            for (LedgerCategoryComponent oldChild : oldCategory.getChildren()) {
                LedgerCategoryComponent newChild = copyLedgerCategory(oldChild, newLedger);
                newChild.setParent(newCategory);
                newCategory.getChildren().add(newChild);
            }

            copy = newCategory;
        } else {
            LedgerSubCategory newSub = new LedgerSubCategory();
            newSub.setName(oldCategory.getName());
            newSub.setLedger(newLedger);
            copy = newSub;
        }

        return copy;
    }


}

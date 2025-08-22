package com.ledger.ledger.business;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.LedgerCategoryComponentRepository;
import com.ledger.ledger.repository.LedgerRepository;
import com.ledger.ledger.repository.TransactionRepository;
import com.ledger.ledger.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/ledger-category-components")
public class LedgerCategoryComponentController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LedgerCategoryComponentRepository ledgerCategoryComponentRepository;

    @Autowired
    private LedgerRepository ledgerRepository;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private TransactionRepository transactionRepository;

    @PostMapping("/ledgers/{ledgerId}/categories")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> createLedgerCategoryComponent(@PathVariable Long ledgerId,
                                                                Principal principal,
                                                                @RequestParam String name,
                                                                @RequestParam CategoryType type){
        User user=userRepository.findByUsername(principal.getName());
        if(user==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }


        LedgerCategoryComponent categoryComponent;

        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new IllegalArgumentException("Ledger not found"));

        if (ledgerCategoryComponentRepository.existsByLedgerAndName(ledger, name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("category already exists in this ledger");
        }

        //crea category
        categoryComponent=new LedgerCategory(name, type, ledger);
        //categoryComponent.setLedger(ledger);
        ledger.addCategoryComponent(categoryComponent);
        ledgerCategoryComponentRepository.save(categoryComponent);

        return ResponseEntity.ok( "Category created successfully");
    }

    @PostMapping("/ledgers/{ledgerId}/categories/{parentId}/subcategories")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> addSubCategoryToCategory (@PathVariable Long ledgerId,
                                                           Principal principal,
                                                           @PathVariable Long parentId,
                                                           @RequestParam String name,
                                                           @RequestParam CategoryType type){
        User user=userRepository.findByUsername(principal.getName());
        if(user==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new IllegalArgumentException("Ledger not found"));

        if (ledgerCategoryComponentRepository.existsByLedgerAndName(ledger, name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Subcategory already exists in this ledger");
        }

        LedgerCategoryComponent parent =ledgerCategoryComponentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("parent not found"));

        if(parent instanceof LedgerSubCategory) {
            return ResponseEntity.badRequest().body("parent must be category");
        }

        if(name== null || name.trim().isEmpty()){
            return ResponseEntity.badRequest().body("name cannot be null");
        }

        LedgerCategoryComponent categoryComponent= new LedgerSubCategory(name, type, ledger);
        parent.add(categoryComponent);
        //categoryComponent.setLedger(ledger);
        ledger.addCategoryComponent(categoryComponent);
        ledgerCategoryComponentRepository.save(categoryComponent);
        ledgerCategoryComponentRepository.save(parent);

        return ResponseEntity.ok("Subcategory created successfully");

    }

    @PutMapping("/{id}/rename")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> renameLedgerCategoryComponent(Principal principal,
                                             @RequestParam String newName,
                                             @PathVariable Long id){

        User user=userRepository.findByUsername(principal.getName());
        if(user==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        LedgerCategoryComponent categoryComponent=ledgerCategoryComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category Component not found"));

        if(newName== null || newName.trim().isEmpty()){
            return ResponseEntity.badRequest().body("new name cannot be null");
        }

        if(ledgerCategoryComponentRepository.existsByLedgerAndName(categoryComponent.getLedger(),newName)){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("new name exists already");
        }

        categoryComponent.setName(newName);
        ledgerCategoryComponentRepository.save(categoryComponent);
        return ResponseEntity.ok("rename successfully");
    }

    @DeleteMapping("/{id}/delete")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deleteLedgerCategoryComponent(@PathVariable Long id,
                                                                Principal principal,
                                                                @RequestParam boolean deleteTransactions,
                                                                @RequestParam(required = false) Long migrateToCategoryId){
        User user=userRepository.findByUsername(principal.getName());
        if(user==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        LedgerCategoryComponent ledgerCategoryComponent=ledgerCategoryComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category Component not found"));

        if (ledgerCategoryComponent instanceof LedgerCategory category && !category.getChildren().isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot delete a category that has subcategories");
        }

        if(!deleteTransactions){ //non cancella transazioni associate a categorycomponent
            if (migrateToCategoryId == null) {
                return ResponseEntity.badRequest().body("Must provide migrateToCategoryId if not deleting transactions");
            }
            LedgerCategoryComponent targetCategory = ledgerCategoryComponentRepository.findById(migrateToCategoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Target category not found"));

            if (targetCategory.getParent() != null) {
                return ResponseEntity.badRequest().body("Target category must be a category");
            }

            for (Transaction t : ledgerCategoryComponent.getTransactions()) {
                t.setCategory(targetCategory);
                targetCategory.addTransaction(t);
                transactionRepository.save(t);
            }
        }else{ //cancalla anche le transazioni
            for (Transaction t : ledgerCategoryComponent.getTransactions()) {
                Account account=t.getAccount();
                account.getTransactions().remove(t);
                if(t instanceof Income){
                    account.debit(t.getAmount());
                }else{
                    account.credit(t.getAmount());
                }

                //t.getAccount().removeTransaction(t);
                t.setAccount(null);

                Ledger ledger=t.getLedger();
                ledger.getTransactions().remove(t);
                t.setLedger(null);

                t.getCategory().getTransactions().remove(t);
                t.setCategory(null);

                transactionRepository.save(t);
                transactionRepository.delete(t);
            }
            //ledgerCategoryComponent.getTransactions().clear();
        }

        ledgerCategoryComponent.getBudgets().clear();

        if (ledgerCategoryComponent instanceof LedgerSubCategory) {
            LedgerCategoryComponent parent = ledgerCategoryComponent.getParent();
            parent.remove(ledgerCategoryComponent);
            ledgerCategoryComponent.setParent(null);
            ledgerCategoryComponentRepository.save(parent);
        }

        Ledger ledger=ledgerCategoryComponent.getLedger();
        ledger.getCategoryComponents().remove(ledgerCategoryComponent);
        ledgerCategoryComponent.setLedger(null);
        ledgerRepository.save(ledger);
        ledgerCategoryComponentRepository.delete(ledgerCategoryComponent);

        return ResponseEntity.ok("deleted successfully");
    }

    @PutMapping("/{id}/demote")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> demoteCategoryToSubCategory (@PathVariable Long id,
                                                               Principal principal,
                                                               @RequestParam Long parentId){
        User user=userRepository.findByUsername(principal.getName());
        if(user==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        LedgerCategoryComponent category=ledgerCategoryComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!(category instanceof LedgerCategory)) {
            return ResponseEntity.badRequest().body("Must be a Category");
        }

        if (parentId == null) {
            return ResponseEntity.badRequest().body("Demote must have parentId");
        }

        LedgerCategoryComponent parent = ledgerCategoryComponentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));

        if(parent instanceof LedgerSubCategory){
            return ResponseEntity.badRequest().body("parent must be Category");
        }

        if (!category.getChildren().isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot demote category with subcategories");
        }


        LedgerCategoryComponent newSub = new LedgerSubCategory(category.getName(), category.getType(), category.getLedger());
        newSub.getTransactions().addAll(category.getTransactions());
        parent.add(newSub);

        ledgerCategoryComponentRepository.delete(category);
        ledgerCategoryComponentRepository.save(newSub);

        return ResponseEntity.ok("Demote successfully");
    }
}

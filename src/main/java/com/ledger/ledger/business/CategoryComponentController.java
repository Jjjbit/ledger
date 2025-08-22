package com.ledger.ledger.business;


import ch.qos.logback.classic.spi.IThrowableProxy;
import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.CategoryComponentRepository;
import com.ledger.ledger.repository.LedgerCategoryComponentRepository;
import com.ledger.ledger.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;


@RestController
@RequestMapping("/category-components")
public class CategoryComponentController {

    @Autowired
    private CategoryComponentRepository categoryComponentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LedgerCategoryComponentRepository ledgerCategoryComponentRepository;

    @PostMapping("/categories")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createCategoryComponent(Principal principal,
                                                          @RequestParam String name,
                                                          @RequestParam CategoryType type,
                                                          @RequestParam (required = false) Long parentId){
        User admin=userRepository.findByUsername(principal.getName());

        if(admin==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }


        if (categoryComponentRepository.findByName(name) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("category already exists");
        }

        CategoryComponent categoryComponent;

        if(parentId == null){
            categoryComponent=new Category(name, type);
        }else {
            CategoryComponent parent =categoryComponentRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            if(parent instanceof SubCategory){
                return ResponseEntity.badRequest().body("parent must be category");
            }

            categoryComponent= new SubCategory(name, type);
            parent.add(categoryComponent);
        }
        categoryComponentRepository.save(categoryComponent);
        return ResponseEntity.ok(parentId == null ? "Category created successfully" : "Subcategory created successfully");
    }


    @PostMapping("/categories/{parentId}/subcategories")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> addSubCategoryToCategory(Principal principal,
                                                    @RequestParam String name,
                                                    @RequestParam CategoryType type,
                                                    @PathVariable Long parentId) {
        User admin = userRepository.findByUsername(principal.getName());

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        CategoryComponent parent = categoryComponentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (parent instanceof SubCategory) {
            return ResponseEntity.badRequest().body("parent must be category");
        }

        CategoryComponent categoryComponent = new SubCategory(name, type);
        parent.add(categoryComponent);

        categoryComponentRepository.save(categoryComponent);

        return ResponseEntity.ok("subcategory created successfully");
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rename(@PathVariable Long id,
                                                        Principal principal,
                                                        @RequestParam String name){
        User admin=userRepository.findByUsername(principal.getName());

        if(admin==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        CategoryComponent categoryComponent=categoryComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Name cannot be null or empty");
        }

        if(categoryComponentRepository.findByName(name) != null && !categoryComponentRepository.findByName(name).getId().equals(categoryComponent.getId())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("category already exists");
        }

        categoryComponent.setName(name);
        categoryComponentRepository.save(categoryComponent);
        return ResponseEntity.ok("category edited successfully");
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCategoryComponent(@PathVariable Long id,
                                                          Principal principal) {
        User admin=userRepository.findByUsername(principal.getName());
        if(admin==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        CategoryComponent categoryComponent=categoryComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        categoryComponentRepository.delete(categoryComponent);

        if (categoryComponent instanceof Category) {
            return ResponseEntity.ok("Category and its subcategories deleted successfully");
        } else{
            return ResponseEntity.ok("Subcategory deleted successfully");
        }
    }

    @PutMapping("/{id}/demote")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> demoteCategoryToSubCategory (@PathVariable Long id,
                                                  Principal principal,
                                                  @RequestParam Long parentId){
        User admin=userRepository.findByUsername(principal.getName());
        if(admin==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        CategoryComponent category=categoryComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!(category instanceof Category)) {
            return ResponseEntity.badRequest().body("Must be a Category");
        }

        if (parentId == null) {
            return ResponseEntity.badRequest().body("Demote must have parentId");
        }

        CategoryComponent parent = categoryComponentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));

        if(parent instanceof SubCategory){
            return ResponseEntity.badRequest().body("parent must be Category");
        }

        if (!category.getChildren().isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot demote category with subcategories");
        }


        SubCategory newSub = new SubCategory(category.getName(), category.getType());
        parent.add(newSub);

        categoryComponentRepository.delete(category);
        categoryComponentRepository.save(newSub);

        return ResponseEntity.ok("Demote successfully");
    }

    @PutMapping("/{id}/promote")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> promoteSubCategory(@PathVariable Long id,
                                                     Principal principal){
        User admin=userRepository.findByUsername(principal.getName());
        if(admin==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        CategoryComponent category=categoryComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!(category instanceof SubCategory)) {
            return ResponseEntity.badRequest().body("Must be a SubCategory");
        }

        if (categoryComponentRepository.findByName(category.getName()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Name already exists");
        }

        Category newCategory = new Category(category.getName(), category.getType());
        categoryComponentRepository.delete(category);
        categoryComponentRepository.save(newCategory);

        return ResponseEntity.ok("Promote successfully");
    }

    @PutMapping("{id}/change-parent")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> changeParent(@PathVariable Long id,
                                               Principal principal,
                                               @RequestParam Long newParentId){
        User admin=userRepository.findByUsername(principal.getName());
        if(admin==null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        CategoryComponent category=categoryComponentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (newParentId == null) {
            return ResponseEntity.badRequest().body("changing parent must have parentId");
        }

        CategoryComponent newParent = categoryComponentRepository.findById(newParentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));

        if(newParent instanceof SubCategory){
            return ResponseEntity.badRequest().body("new parent must be Category");
        }

        if(category instanceof Category){
            return ResponseEntity.badRequest().body("Cannot change parent of category");
        }

        category.getParent().getChildren().remove(category); //rimuove da parente vecchio
        //category.setParent(newParent);
        newParent.add(category); //aggiunge a parente nuovo
        //((SubCategory)category).changeParent(newParent);
        categoryComponentRepository.save(category);

        return ResponseEntity.ok("change parent successfully");
    }


}

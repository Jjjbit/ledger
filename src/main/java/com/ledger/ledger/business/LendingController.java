package com.ledger.ledger.business;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

//TODO
@RestController
@RequestMapping("/lendings")
public class LendingController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private LedgerRepository ledgerRepository;


    @DeleteMapping("/{id}/delete")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> deleteLending(@PathVariable Long id,
                                                Principal principal,
                                                @RequestParam boolean deleteRecord) {

        User owner=userRepository.findByUsername(principal.getName());
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        return ResponseEntity.ok("Lending deleted successfully");
    }
}

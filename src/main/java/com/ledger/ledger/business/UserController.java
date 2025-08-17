package com.ledger.ledger.business;

import com.ledger.ledger.domain.Account;
import com.ledger.ledger.domain.PasswordUtils;
import com.ledger.ledger.domain.User;
import com.ledger.ledger.repository.AccountRepository;
import com.ledger.ledger.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<String> register(@RequestParam String username, @RequestParam String password) {
        if (userRepository.findByUsername(username) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }
        User user = new User(username, PasswordUtils.hash(password));
        userRepository.save(user);
        return ResponseEntity.ok("Registration successful");
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        User existingUser = userRepository.findByUsername(username);
        if (existingUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        if (!PasswordUtils.verify(password, existingUser.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password incorrect");
        }
        return ResponseEntity.ok("Login successful");
    }


    @PutMapping("/{id}")
    @Transactional
    public String updateUserInfo(@PathVariable Long id, @RequestParam String username, @RequestParam String password) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return "User not found";
        }
        user.setUsername(username);
        if (password != null && !password.isEmpty()) {
            user.setPassword(PasswordUtils.hash(password));
        }
        userRepository.save(user);
        return "User information updated successfully";
    }

    @GetMapping("/me/accounts")
    public List<Account> getMyAccounts(Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        return accountRepository.findByOwner(user);
    }

}

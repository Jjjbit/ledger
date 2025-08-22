package com.ledger.ledger.business;

import com.ledger.ledger.domain.Account;
import com.ledger.ledger.domain.PasswordUtils;
import com.ledger.ledger.domain.User;
import com.ledger.ledger.repository.AccountRepository;
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
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<String> register(@RequestParam String username,
                                           @RequestParam String password) {
        if (userRepository.findByUsername(username) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }
        User user = new User(username, PasswordUtils.hash(password),  User.Role.USER);
        userRepository.save(user);
        return ResponseEntity.ok("Registration successful");
    }

    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<String> createAdmin(@RequestParam String username,
                                              @RequestParam String password,
                                              Principal principal) {
        User adminCreator = userRepository.findByUsername(principal.getName());
        if (adminCreator == null || adminCreator.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized");
        }

        if (userRepository.findByUsername(username) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }
        User admin = new User(username, PasswordUtils.hash(password), User.Role.ADMIN);
        userRepository.save(admin);
        return ResponseEntity.ok("Admin account created successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username,
                                        @RequestParam String password) {
        User existingUser = userRepository.findByUsername(username);
        if (existingUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        if (!PasswordUtils.verify(password, existingUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password incorrect");
        }
        return ResponseEntity.ok("Login successful");
    }


    @PutMapping("/me")
    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<String> updateUserInfo(Principal principal,
                                 @RequestParam (required = false) String username,
                                 @RequestParam (required = false) String password) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
        }
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        if(username != null){user.setUsername(username);}
        if (password != null && !password.isEmpty()) {
            user.setPassword(PasswordUtils.hash(password));
        }
        userRepository.save(user);
        return ResponseEntity.ok("User info updated");
    }

    @GetMapping("/me")
    //@PreAuthorize("hasRole('USER')")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')") // solo user autenticato permettono di accedere questa interfaccia. ritorna info di uetente/admin
    public ResponseEntity<User> getAuthenticatedUser(Principal principal) { //return current user. principal offre name di user corrente
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(user);
    }


    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> updateUserAsAdmin(Principal principal, //admin modifica info di user
                                                    @PathVariable Long id,
                                                    @RequestParam(required = false) String username,
                                                    @RequestParam(required = false) String password) {
        User currentUser = userRepository.findByUsername(principal.getName());
        if (currentUser == null || !currentUser.getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        if (username != null && !username.isEmpty()) user.setUsername(username);
        if (password != null && !password.isEmpty()) user.setPassword(PasswordUtils.hash(password));
        userRepository.save(user);
        return ResponseEntity.ok("User info updated by admin");
    }

    @GetMapping("/role-check")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> checkRole(Principal principal) { //individua ruolo
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");

        User user = userRepository.findByUsername(principal.getName());
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");

        if (user.getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.ok("You are an admin");
        } else {
            return ResponseEntity.ok("You are a normal user");
        }
    }

    @GetMapping("/me/accounts")
    public List<Account> getMyAccounts(Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        return accountRepository.findByOwner(user);
    }

}

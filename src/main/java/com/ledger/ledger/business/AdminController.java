package com.ledger.ledger.business;

import com.ledger.ledger.domain.PasswordUtils;
import com.ledger.ledger.domain.User;
import com.ledger.ledger.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<String> register(@RequestParam String username,
                                           @RequestParam String password) {
        if (userRepository.findByUsername(username) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }
        User user = new User(username, PasswordUtils.hash(password),  User.Role.ADMIN);
        userRepository.save(user);
        return ResponseEntity.ok("Registration admin successful");
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
        return ResponseEntity.ok("Login admin successful");
    }

}

package com.ledger.ledger.service;

import com.ledger.ledger.domain.PasswordUtils;
import com.ledger.ledger.domain.User;
import com.ledger.ledger.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public void register(String username, String password) {
        if (userRepository.findByUsername(username)!= null) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = new User(username, PasswordUtils.hash(password));
        userRepository.save(user);

    }

    public boolean login(String username, String plainPassword) {
        User user = userRepository.findByUsername(username);
        return user != null && PasswordUtils.verify(plainPassword, user.getPasswordHash());
    }

    public boolean updateUser(User user) {
        return userRepository.findById(user.getId()).map(u -> {
            u.setUsername(u.getUsername());
            if (u.getPasswordHash() != null && !u.getPasswordHash().isBlank()) {
                u.setPassword(PasswordUtils.hash(u.getPasswordHash()));
            }
            userRepository.save(u);
            return true;
        }).orElse(false);
    }


}

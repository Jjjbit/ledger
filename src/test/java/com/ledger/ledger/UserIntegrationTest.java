package com.ledger.ledger;

import com.ledger.ledger.domain.PasswordUtils;
import com.ledger.ledger.domain.User;
import com.ledger.ledger.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.ledger.ledger.LedgerApplication.class)
@Transactional
@AutoConfigureMockMvc
@Rollback
public class UserIntegrationTest { // Integration test between UserController and UserRepository. collega a database reale
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testRegisterAndVerifyInDatabase() throws Exception {

        String username = "newuser";
        String password = "secure123";

        mockMvc.perform(post("/users/register")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("Registration successful"));

        User savedUser = userRepository.findByUsername(username);


        Assertions.assertNotNull(savedUser);
        Assertions.assertTrue(PasswordUtils.verify("secure123", savedUser.getPassword()));
        Assertions.assertEquals(User.Role.USER, savedUser.getRole());
    }

    @Test
    public void testRegisterDuplicateUsername() throws Exception {
        User user=new User("duplicate", "pass123", User.Role.USER);
        userRepository.save(user);

        mockMvc.perform(post("/users/register")
                        .param("username", "duplicate")
                        .param("password", "pass123"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Username already exists"));


    }

    @Test
    public void testLoginWithCorrectCredentials() throws Exception {
        User user = new User("testuser", PasswordUtils.hash("securepassword"), User.Role.USER);
        userRepository.save(user);

        mockMvc.perform(post("/users/login")
                        .param("username", "testuser")
                        .param("password", "securepassword"))
                .andExpect(status().isOk())
                .andExpect(content().string("Login successful"));

        User existingUser = userRepository.findByUsername("testuser");
        Assertions.assertNotNull(existingUser);
        Assertions.assertTrue(PasswordUtils.verify("securepassword", existingUser.getPassword()));
    }

    @Test
    public void testLoginWithIncorrectCredentials() throws Exception {
        User user = new User("testuser", PasswordUtils.hash("securepassword"), User.Role.USER);
        userRepository.save(user);

        mockMvc.perform(post("/users/login")
                        .param("username", "testuser")
                        .param("password", "wrongpassword"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Password incorrect"));

        User existingUser = userRepository.findByUsername("testuser");
        Assertions.assertNotNull(existingUser);
        Assertions.assertTrue(PasswordUtils.verify("securepassword", existingUser.getPassword()));
    }

}

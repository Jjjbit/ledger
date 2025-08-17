package com.ledger.ledger;

import com.ledger.ledger.business.UserController;
import com.ledger.ledger.domain.PasswordUtils;
import com.ledger.ledger.domain.User;
import com.ledger.ledger.repository.AccountRepository;
import com.ledger.ledger.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
public class UserControllerTest { //test UserController. non collega a database, usa mockito per simulare il comportamento del repository

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        public AccountRepository accountRepository() {
            return Mockito.mock(AccountRepository.class);
        }
    }


    @Test
    public void testRegisterSuccess() throws Exception {
        String username = "newuser";
        String password = "secure123";

        Mockito.when(userRepository.findByUsername(username)).thenReturn(null);
        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenReturn(new User(username, PasswordUtils.hash(password)));

        mockMvc.perform(post("/users/register")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("Registration successful"));
    }

    @Test
    public void testRegisterConflict() throws Exception {
        String username = "existinguser";
        String password = "123456";

        Mockito.when(userRepository.findByUsername(username))
                .thenReturn(new User(username, PasswordUtils.hash(password)));


        mockMvc.perform(post("/users/register")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isConflict())
                .andExpect(content().string("Username already exists"));
    }

    @Test
    public void testLoginSuccess() throws Exception {
        String username = "newuser";
        String password = "secure123";
        String hashedPassword = PasswordUtils.hash(password);

        Mockito.when(userRepository.findByUsername(username))
                .thenReturn(new User(username, hashedPassword));

        mockMvc.perform(post("/users/login")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("Login successful"));
    }

    @Test
    public void testLoginUserNotFound() throws Exception {
        String username = "unknownuser";
        String password = "password";

        Mockito.when(userRepository.findByUsername(username))
                .thenReturn(null);

        mockMvc.perform(post("/users/login")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("User not found"));
    }

    @Test
    public void testLoginIncorrectPassword() throws Exception {
        String username = "existinguser";
        String password = "wrongpassword";
        String correctPassword = "correctpassword";
        String hashedPassword = PasswordUtils.hash(correctPassword);

        Mockito.when(userRepository.findByUsername(username))
                .thenReturn(new User(username, hashedPassword));

        mockMvc.perform(post("/users/login")
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(content().string("Password incorrect"));
    }

    @Test
    public void testUpdateUserInfoSuccess() throws Exception {
        Long userId = 1L;
        String newUsername = "updateduser";
        String newPassword = "newpassword";
        User existingUser = new User("existinguser", PasswordUtils.hash("oldpassword"));
        existingUser.setId(userId);

        Mockito.when(userRepository.findById(userId)).thenReturn(
                java.util.Optional.of(existingUser));
        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/users/" + userId)
                        .param("username", newUsername)
                        .param("password", newPassword))
                .andExpect(status().isOk())
                .andExpect(content().string("User information updated successfully"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        Assertions.assertEquals(newUsername, savedUser.getUsername());
        Assertions.assertTrue(PasswordUtils.verify(newPassword, savedUser.getPasswordHash()));
    }
}

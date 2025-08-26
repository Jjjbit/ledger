package com.ledger.ledger;

import com.ledger.ledger.business.AccountController;
import com.ledger.ledger.business.UserController;
import com.ledger.ledger.domain.Account;
import com.ledger.ledger.domain.BasicAccount;
import com.ledger.ledger.domain.User;
import com.ledger.ledger.repository.AccountRepository;
import com.ledger.ledger.repository.TransactionRepository;
import com.ledger.ledger.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;


import java.math.BigDecimal;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;


@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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

        @Bean
        public TransactionRepository transactionRepository() {
            return Mockito.mock(TransactionRepository.class);
        }
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testCreateBasicAccount() throws Exception {
        User user = new User("testuser", "securepassword", User.Role.USER);
        userRepository.save(user);

        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(user);
        Mockito.when(accountRepository.save(Mockito.any(Account.class)))
                .thenAnswer(invocation -> {
                    Account acc = invocation.getArgument(0);
                    acc.setId(1L);
                    return acc;
                });// Simulating save operation


        //simula richiesta di HTTP POST per creare un account
        mockMvc.perform(post("/accounts/create-basic-account") //URL della richiesta
                        .param("accountName", "Test Account") //simula parametri della richiesta
                        .param("balance", "1000")
                        .principal(() -> "testuser") //simula autenticazione dell'utente
                        .param("note", "Test note")
                        .param("includedInNetWorth", "true")
                        .param("selectable", "true")
                        .param("type", "CASH")
                        .param("category", "FUNDS"))
                        .andExpect(status().isOk())
                        .andExpect(content().string("Basic account created successfully")); //Verifica che la risposta del controller sia conforme alle aspettative
    }

    @Test
    public void testCreateCreditAccount() throws Exception {
        User user = new User("testuser", "securepassword", User.Role.USER);
        userRepository.save(user);

        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(user);
        Mockito.when(accountRepository.save(Mockito.any(Account.class)))
                .thenAnswer(invocation -> {
                    Account acc = invocation.getArgument(0);
                    acc.setId(1L);
                    return acc;
                });
    }
}

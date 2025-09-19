package com.ledger.ledger;

import com.ledger.ledger.business.BudgetController;
import com.ledger.ledger.domain.Budget;
import com.ledger.ledger.domain.User;
import com.ledger.ledger.repository.AccountRepository;
import com.ledger.ledger.repository.BudgetRepository;
import com.ledger.ledger.repository.TransactionRepository;
import com.ledger.ledger.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class BudgetControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetController budgetController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(budgetController).build();
    }



    @Test
    @WithMockUser(username = "Alice")
    public  void testGetBudgets_userHasBudget() throws Exception {
        User user = new User("Alice", "pass123");
        userRepository.save(user);

        Budget userBudget = new Budget(BigDecimal.valueOf(2000), Budget.BudgetPeriod.MONTHLY, null, user);
        budgetRepository.save(userBudget);
        user.getBudgets().add(userBudget);

        Mockito.when(userRepository.findByUsername("Alice")).thenReturn(user);
        Mockito.when(budgetRepository.findActiveUserBudget(user.getId(), LocalDate.now())).thenReturn(Optional.of(userBudget));

        Mockito.when(transactionRepository.sumExpensesByUserAndPeriod(eq(user.getId()), any(), any()))
                .thenReturn(BigDecimal.valueOf(1200));
        Mockito.when(budgetRepository.findActiveCategoriesBudgetByUserId(user.getId(), LocalDate.now()))
                .thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/budgets")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userBudget.amount").value(2000))
                .andExpect(jsonPath("$.userBudget.spent").value(1200))
                .andExpect(jsonPath("$.userBudget.remaining").value(800));
    }


}

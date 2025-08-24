package com.ledger.ledger;

import com.ledger.ledger.domain.*;
import com.ledger.ledger.domain.LedgerCategoryComponent;
import com.ledger.ledger.repository.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest(classes = com.ledger.ledger.LedgerApplication.class)
@Transactional
@AutoConfigureMockMvc
@Rollback
public class AccountTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerCategoryComponentRepository ledgerCategoryComponentRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InstallmentPlanRepository installmentPlanRepository;

    private Ledger testLedger;
    private User testUser;
    private LedgerCategoryComponent foodCategory;
    private LedgerCategoryComponent salaryCategory;

    @BeforeEach
    public void setUp() {
        testUser = new User("Alice", "password123", User.Role.USER);
        userRepository.save(testUser);

        testLedger = new Ledger("Test Ledger", testUser);
        ledgerRepository.save(testLedger);

        foodCategory = new LedgerCategory("Food", CategoryType.EXPENSE, testLedger);
        ledgerCategoryComponentRepository.save(foodCategory);

        salaryCategory = new LedgerCategory("Salary", CategoryType.INCOME, testLedger);
        ledgerCategoryComponentRepository.save(salaryCategory);


    }

    @Test
    @WithMockUser(username = "Alice") // Simulating an authenticated user
    public void testCreateBasicAccount() throws Exception {

        mockMvc.perform(post("/accounts/create-basic-account")
                        .param("accountName", "Test Account")
                        .param("balance", "1000")
                        .principal(() -> "Alice")
                        .param("note", "Test note")
                        .param("includedInNetWorth", "true")
                        .param("selectable", "true")
                        .param("type", "CASH")
                        .param("category", "FUNDS"))
                .andExpect(status().isOk())
                .andExpect(content().string("Basic account created successfully"));

        Account createdAccount = accountRepository.findByName("Test Account");
        Assertions.assertNotNull(createdAccount);

        User user = userRepository.findByUsername(testUser.getUsername());
        Assertions.assertEquals(new BigDecimal("1000"), user.calculateTotalAssets());

    }

    @Test
    @WithMockUser(username = "Alice")
    public void  testCreateCreditAccount() throws Exception{
        mockMvc.perform(post("/accounts/create-credit-account")
                        .param("accountName", "Test Account")
                        .param("balance", "1000")
                        .principal(() -> "Alice")
                        .param("note", "Test note")
                        .param("includedInNetWorth", "true")
                        .param("selectable", "true")
                        .param("type", "CREDIT_CARD")
                        .param("creditLimit", "100")
                        .param("currentDebt", "")
                        .param("billDate", "")
                        .param("dueDate", ""))
                .andExpect(status().isOk())
                .andExpect(content().string("Credit account created successfully"));

        Account createdAccount = accountRepository.findByName("Test Account");
        Assertions.assertNotNull(createdAccount);

        Assertions.assertEquals(new BigDecimal("0"), ((CreditAccount)createdAccount).getCurrentDebt());
        Assertions.assertNull(((CreditAccount)createdAccount).getBillDay());
        Assertions.assertNull(((CreditAccount)createdAccount).getDueDay());

        User updateUser= userRepository.findByUsername(testUser.getUsername());
        Assertions.assertEquals(new BigDecimal("1000"), updateUser.getTotalAssets());
        Assertions.assertEquals(new BigDecimal("0"), updateUser.getTotalLiabilities());
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testCreateLoanAccount() throws Exception{
        Account account = new BasicAccount("receiving account",
                BigDecimal.valueOf(1000),
                null,
                true,
                true,
                AccountType.CASH,
                AccountCategory.FUNDS,
                testUser);
        accountRepository.save(account);

        mockMvc.perform(post("/accounts/create-loan-account")
                        .param("accountName", "Test Account")
                        .principal(() -> "Alice")
                        .param("note", "Test note")
                        .param("includedInNetAsset", "true")
                        .param("receivingAccountId", account.getId().toString())
                        .param("totalPeriods", "36")
                        .param("repaidPeriods", "1")
                        .param("annualInterestRate", "1")
                        .param("loanAmount", "100")
                        .param("repaymentDate", "2025-08-19")
                        .param("repaymentType", "EQUAL_INTEREST"))
                .andExpect(status().isOk())
                .andExpect(content().string("Loan account created successfully"));

        Account createdAccount =accountRepository.findByName("Test Account");
        Assertions.assertNotNull(createdAccount);

        User updateUser= userRepository.findByUsername(testUser.getUsername());
        Assertions.assertEquals(new BigDecimal("1100"), updateUser.getTotalAssets());
        Assertions.assertEquals(new BigDecimal("98.70"), updateUser.getTotalLiabilities());
        Assertions.assertEquals(new BigDecimal("1001.30"), updateUser.getNetAssets());
    }


    @Test
    @WithMockUser(username = "Alice") // Simulating an authenticated user
    public void testDeleteCreditAccountWithTransactions() throws Exception {
        Account account = new CreditAccount("Test Account",
                BigDecimal.valueOf(1000),
                testUser,
                null,
                true,
                true,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(100), // current debt
                null,
                null,
                AccountType.CREDIT_CARD
        );
        accountRepository.save(account);

        InstallmentPlan installmentPlan = new InstallmentPlan(
                BigDecimal.valueOf(1200), // total amount
                12, // total periods
                BigDecimal.valueOf(0), // fee rate
                1, // repaid periods
                InstallmentPlan.FeeStrategy.EVENLY_SPLIT,
                account // linked account
        );
        installmentPlanRepository.save(installmentPlan);
        ((CreditAccount) account).addInstallmentPlan(installmentPlan);

        // Add transactions to the account
        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, account, testLedger, foodCategory);
        Transaction transaction2 = new Income(LocalDate.now(), BigDecimal.valueOf(1500), null, account, testLedger, salaryCategory);
        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);

        account.addTransaction(transaction1);
        account.addTransaction(transaction2);

        accountRepository.save(account);

        mockMvc.perform(delete("/accounts/" + account.getId())
                        .param("deleteTransactions", "true")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account and associated transactions deleted successfully"));

        Account deletedAccount1 = accountRepository.findById(account.getId()).orElse(null);
        Assertions.assertNull(deletedAccount1);

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(0, updatedUser.getTotalAssets().intValue());
        Assertions.assertEquals(0, updatedUser.getTotalLiabilities().intValue());
        Assertions.assertEquals(0, updatedUser.getNetAssets().intValue());

        InstallmentPlan plan = installmentPlanRepository.findById(installmentPlan.getId()).orElse(null);
        Assertions.assertNull(plan);

        Transaction deletedTransaction1 = transactionRepository.findById(transaction1.getId()).orElse(null);
        Assertions.assertNull(deletedTransaction1);

        Transaction deletedTransaction2 = transactionRepository.findById(transaction2.getId()).orElse(null);
        Assertions.assertNull(deletedTransaction2);

        Assertions.assertEquals(0, transactionRepository.findByLedgerId(testLedger.getId()).size());
        Assertions.assertEquals(0, transactionRepository.findByCategoryId(foodCategory.getId()).size());
        Assertions.assertEquals(0, transactionRepository.findByCategoryId(salaryCategory.getId()).size());
    }

    @Test
    @WithMockUser(username = "Alice") // Simulating an authenticated user
    public void testDeleteCreditAccountWithoutTransactions() throws Exception{
        Account account = new CreditAccount("Test Account",
                BigDecimal.valueOf(1000),
                testUser,
                null,
                true,
                true,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(100), // current debt
                null,
                null,
                AccountType.CREDIT_CARD
        );
        accountRepository.save(account);

        InstallmentPlan installmentPlan = new InstallmentPlan(
                BigDecimal.valueOf(1200), // total amount
                12, // total periods
                BigDecimal.valueOf(0), // fee rate
                1, // repaid periods
                InstallmentPlan.FeeStrategy.EVENLY_SPLIT,
                account // linked account
        );
        installmentPlanRepository.save(installmentPlan);
        ((CreditAccount) account).addInstallmentPlan(installmentPlan);

        // Add transactions to the account
        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, account, testLedger, foodCategory);
        Transaction transaction2 = new Income(LocalDate.now(), BigDecimal.valueOf(1500), null, account, testLedger, salaryCategory);
        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);

        account.addTransaction(transaction1);
        account.addTransaction(transaction2);

        accountRepository.save(account);

        mockMvc.perform(delete("/accounts/" + account.getId())
                        .param("deleteTransactions", "false")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account disassociated from transactions and deleted successfully"));

        Account deletedAccount = accountRepository.findById(account.getId()).orElse(null); //cerca account da database
        Assertions.assertNull(deletedAccount);

        InstallmentPlan plan = installmentPlanRepository.findById(installmentPlan.getId()).orElse(null);
        Assertions.assertNull(plan);

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(0, updatedUser.getTotalAssets().compareTo(new BigDecimal("1490")));
        Assertions.assertEquals(0, updatedUser.getTotalLiabilities().compareTo(BigDecimal.ZERO));
        Assertions.assertEquals(0, updatedUser.getNetAssets().compareTo(new BigDecimal("1490")));

        Transaction updatedTransaction1 = transactionRepository.findById(transaction1.getId()).orElse(null);
        Assertions.assertNotNull(updatedTransaction1);

        Transaction updatedTransaction2 = transactionRepository.findById(transaction2.getId()).orElse(null);
        Assertions.assertNotNull(updatedTransaction2);

        Assertions.assertEquals(2, transactionRepository.findByLedgerId(testLedger.getId()).size());
        Assertions.assertEquals(1, transactionRepository.findByCategoryId(foodCategory.getId()).size());
        Assertions.assertEquals(1, transactionRepository.findByCategoryId(salaryCategory.getId()).size());
    }


    //TODO: test delete LoanAccount

    @Test
    @WithMockUser(username = "Alice") // Simulating an authenticated user
    public void testDeleteBasicAccountWithTransactions() throws Exception {
        Account account = new BasicAccount("Test Account",
                BigDecimal.valueOf(1000),
                null,
                true,
                true,
                AccountType.CASH,
                AccountCategory.FUNDS,
                testUser);

        accountRepository.save(account);

        // Add transactions to the account
        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, account, testLedger, foodCategory);
        Transaction transaction2 = new Income(LocalDate.now(), BigDecimal.valueOf(1500), null, account, testLedger, salaryCategory);
        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);

        account.addTransaction(transaction1);
        account.addTransaction(transaction2);

        accountRepository.save(account);

        mockMvc.perform(delete("/accounts/" + account.getId())
                        .param("deleteTransactions", "true")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account and associated transactions deleted successfully"));

        Account deletedAccount = accountRepository.findById(account.getId()).orElse(null); //cerca account da database
        Assertions.assertNull(deletedAccount);

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(0, updatedUser.getTotalAssets().intValue());

        Transaction deletedTransaction1 = transactionRepository.findById(transaction1.getId()).orElse(null);
        Assertions.assertNull(deletedTransaction1);

        Transaction deletedTransaction2 = transactionRepository.findById(transaction2.getId()).orElse(null);
        Assertions.assertNull(deletedTransaction2);

        Assertions.assertEquals(0, transactionRepository.findByLedgerId(testLedger.getId()).size());
        Assertions.assertEquals(0, transactionRepository.findByCategoryId(foodCategory.getId()).size());
        Assertions.assertEquals(0, transactionRepository.findByCategoryId(salaryCategory.getId()).size());
    }

    @Test
    @WithMockUser(username = "Alice") // Simulating an authenticated user
    public void testDeleteBasicAccountWithoutTransactions() throws Exception {
        Account account = new BasicAccount("Test Account",
                BigDecimal.valueOf(1000),
                null,
                true,
                true,
                AccountType.CASH,
                AccountCategory.FUNDS,
                testUser);
        accountRepository.save(account);

        // Add transactions to the account
        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, account, testLedger, foodCategory);
        Transaction transaction2 = new Income(LocalDate.now(), BigDecimal.valueOf(1500), null, account, testLedger, salaryCategory);
        transactionRepository.save(transaction1);
        transactionRepository.save(transaction2);


        account.addTransaction(transaction1);
        account.addTransaction(transaction2);

        accountRepository.save(account);

        mockMvc.perform(delete("/accounts/" + account.getId())
                        .param("deleteTransactions", "false")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account disassociated from transactions and deleted successfully"));

        Account deletedAccount = accountRepository.findById(account.getId()).orElse(null); //cerca account da database
        Assertions.assertNull(deletedAccount);

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(0, updatedUser.getTotalAssets().compareTo(new BigDecimal("1490")));

        Transaction updatedTransaction1 = transactionRepository.findById(transaction1.getId()).orElse(null);
        Assertions.assertNotNull(updatedTransaction1);

        Transaction updatedTransaction2 = transactionRepository.findById(transaction2.getId()).orElse(null);
        Assertions.assertNotNull(updatedTransaction2);

        Assertions.assertEquals(2, transactionRepository.findByLedgerId(testLedger.getId()).size());

        Assertions.assertEquals(1, transactionRepository.findByCategoryId(foodCategory.getId()).size());

        Assertions.assertEquals(1, transactionRepository.findByCategoryId(salaryCategory.getId()).size());

    }

    @Test
    @WithMockUser(username = "Alice") // Simulating an authenticated user
    public void testHideAccount() throws Exception {
        Account account = new BasicAccount("Test Account",
                BigDecimal.valueOf(1000),
                null,
                true,
                true,
                AccountType.CASH,
                AccountCategory.FUNDS,
                testUser);
        accountRepository.save(account);

        mockMvc.perform(put("/accounts/" + account.getId() + "/hide")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account hidden successfully"));


        Account updatedAccount = accountRepository.findById(account.getId()).orElse(null);
        Assertions.assertTrue(updatedAccount.getHidden());

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(0, updatedUser.getTotalAssets().intValue());
    }

    @Test
    @WithMockUser(username = "Alice") // Simulating an authenticated user
    public void testEditBasicAccount() throws Exception {
        Account account = new BasicAccount("Test Account",
                BigDecimal.valueOf(1000),
                null,
                true,
                true,
                AccountType.CASH,
                AccountCategory.FUNDS,
                testUser);
        accountRepository.save(account);

        mockMvc.perform(put("/accounts/" + account.getId() + "/edit-basic-account")
                        .param("name", "Updated Account")
                        .param("balance", "1500")
                        .param("notes", "Updated note")
                        .param("includedInNetAsset", "false")
                        .param("selectable", "false")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account edited successfully"));

        Account updatedAccount = accountRepository.findById(account.getId()).orElse(null);
        Assertions.assertNotNull(updatedAccount);

        Assertions.assertEquals("Updated Account", updatedAccount.getName());

        Assertions.assertEquals(BigDecimal.valueOf(1500), updatedAccount.getBalance());

        Assertions.assertEquals("Updated note", updatedAccount.getNotes());

        Assertions.assertFalse(updatedAccount.getIncludedInNetAsset());

        Assertions.assertFalse(updatedAccount.getSelectable());

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(0, updatedUser.getTotalAssets().intValue());

    }

    @Test
    @WithMockUser(username = "Alice")
    public void testEditCreditAccount() throws Exception{
        Account account = new CreditAccount("Test Account",
                BigDecimal.valueOf(1000),
                testUser,
                null,
                true,
                true,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(0),
                null,
                null,
                AccountType.CREDIT_CARD
                );
        accountRepository.save(account);

        mockMvc.perform(put("/accounts/" + account.getId() + "/edit-credit-account")
                        .param("name", "Updated Account")
                        .param("balance", "1500")
                        .param("notes", "Updated note")
                        .param("includedInNetAsset", "true")
                        .param("selectable", "false")
                        .param("creditLimit", "900")
                        .param("currentDebt", "10")
                        .param("billDate", "1")
                        .param("dueDate", "")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Credit account edited successfully"));

        Account updatedAccount = accountRepository.findById(account.getId()).orElse(null);
        Assertions.assertNotNull(updatedAccount);

        Assertions.assertEquals(new BigDecimal("900"), ((CreditAccount) updatedAccount).getCreditLimit());

        Assertions.assertEquals(new BigDecimal("10"), ((CreditAccount) updatedAccount).getCurrentDebt());

        Assertions.assertEquals(1, ((CreditAccount) updatedAccount).getBillDay());

        Assertions.assertNull(((CreditAccount) updatedAccount).getDueDay());

        Assertions.assertEquals("Updated Account", updatedAccount.getName());

        Assertions.assertEquals(BigDecimal.valueOf(1500), updatedAccount.getBalance());

        Assertions.assertEquals("Updated note", updatedAccount.getNotes());

        Assertions.assertTrue(updatedAccount.getIncludedInNetAsset());

        Assertions.assertFalse(updatedAccount.getSelectable());

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(new BigDecimal("1500"), updatedUser.getTotalAssets());
        Assertions.assertEquals(new BigDecimal("10"), updatedUser.getTotalLiabilities());
        Assertions.assertEquals(new BigDecimal("1490"), updatedUser.getNetAssets());
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testEditLoanAccount() throws Exception{
        Account account = new LoanAccount("Test Account",
                testUser,
                null,
                true,
                36,
                0,
                BigDecimal.valueOf(0),
                BigDecimal.valueOf(100),
                null,
                LocalDate.now(),
                LoanAccount.RepaymentType.EQUAL_INTEREST
        );
        accountRepository.save(account);

        mockMvc.perform(put("/accounts/" + account.getId() + "/edit-loan-account")
                        .param("name", "Updated Account")
                        .param("notes", "Updated note")
                        .param("includedInNetAsset", "true")
                        .param("selectable", "false")
                        .param("totalPeriods", "" )
                        .param("repaidPeriods", "1")
                        .param("annualInterestRate", "1")
                        .param("loanAmount", "")
                        .param("repaymentDate", "")
                        .param("repaymentType", "")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Loan account edited successfully"));

        Account updatedAccount = accountRepository.findById(account.getId()).orElse(null);
        Assertions.assertNotNull(updatedAccount);

        Assertions.assertEquals(1, ((LoanAccount) updatedAccount).getRepaidPeriods());

        Assertions.assertEquals(36, ((LoanAccount) updatedAccount).getTotalPeriods());

        Assertions.assertEquals("Updated Account", updatedAccount.getName());

        Assertions.assertEquals("Updated note", updatedAccount.getNotes());

        Assertions.assertTrue(updatedAccount.getIncludedInNetAsset());

        Assertions.assertFalse(updatedAccount.getSelectable());

        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(0, updatedUser.getTotalAssets().intValue());
        Assertions.assertEquals(new BigDecimal("98.70"), updatedUser.getTotalLiabilities());
        Assertions.assertEquals(new BigDecimal("-98.70"), updatedUser.getNetAssets());
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testCreditAccount() throws Exception{
        //test credit BasicAccount
        Account basicAccount = new BasicAccount("account1",
                BigDecimal.valueOf(1000),
                null,
                true,
                true,
                AccountType.CASH,
                AccountCategory.FUNDS,
                testUser);
        accountRepository.save(basicAccount);

        mockMvc.perform(put("/accounts/" + basicAccount.getId() + "/credit")
                        .param("amount", "10")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("credit account"));

        Account updateAccount=accountRepository.findByName("account1");
        Assertions.assertNotNull(updateAccount);
        Assertions.assertEquals(new BigDecimal("1010"), updateAccount.getBalance());


        //test credit CreditAccount
        Account creditAccount = new CreditAccount("account2",
                BigDecimal.valueOf(1000),
                testUser,
                null,
                true,
                true,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(0),
                null,
                null,
                AccountType.CREDIT_CARD
        );
        accountRepository.save(creditAccount);

        mockMvc.perform(put("/accounts/" + creditAccount.getId() + "/credit")
                        .param("amount", "10")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("credit account"));

        Account updateAccount2=accountRepository.findByName("account2");
        Assertions.assertNotNull(updateAccount2);
        Assertions.assertEquals(new BigDecimal("1010"), updateAccount2.getBalance());

        User updateUser=userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(new BigDecimal("2020"), updateUser.getTotalAssets());

        //test credit LoanAccount
        Account loanAccount = new LoanAccount("account3",
                testUser,
                null,
                true,
                36,
                0,
                BigDecimal.valueOf(0),
                BigDecimal.valueOf(100),
                null,
                LocalDate.now(),
                LoanAccount.RepaymentType.EQUAL_INTEREST
        );
        accountRepository.save(loanAccount);

        mockMvc.perform(put("/accounts/" + loanAccount.getId() + "/credit")
                        .param("amount", "10")
                        .principal(() -> "Alice"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cannot credit a loan account"));
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testDebitAccount() throws Exception{
        //test credit BasicAccount
        Account basicAccount = new BasicAccount("account1",
                BigDecimal.valueOf(1000),
                null,
                true,
                true,
                AccountType.CASH,
                AccountCategory.FUNDS,
                testUser);
        accountRepository.save(basicAccount);

        mockMvc.perform(put("/accounts/" + basicAccount.getId() + "/debit")
                        .param("amount", "10")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("debit account"));

        Account updateAccount=accountRepository.findByName("account1");
        Assertions.assertNotNull(updateAccount);
        Assertions.assertEquals(new BigDecimal("990"), updateAccount.getBalance());

        //test credit CreditAccount with balance=0
        Account creditAccount = new CreditAccount("account2",
                BigDecimal.valueOf(0),
                testUser,
                null,
                true,
                true,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(0),
                null,
                null,
                AccountType.CREDIT_CARD
        );
        accountRepository.save(creditAccount);

        mockMvc.perform(put("/accounts/" + creditAccount.getId() + "/debit")
                        .param("amount", "10")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("debit account"));

        Account updateAccount2=accountRepository.findByName("account2");
        Assertions.assertNotNull(updateAccount2);
        Assertions.assertEquals(new BigDecimal("0"), updateAccount2.getBalance());
        Assertions.assertEquals(new BigDecimal("10"), ((CreditAccount)updateAccount2).getCurrentDebt());

        User updateUser=userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(new BigDecimal("990"), updateUser.getTotalAssets());
        Assertions.assertEquals(new BigDecimal("10"), updateUser.getTotalLiabilities());
        Assertions.assertEquals(new BigDecimal("980"), updateUser.getNetAssets());

        //test credit LoanAccount
        Account loanAccount = new LoanAccount("account3",
                testUser,
                null,
                true,
                36,
                0,
                BigDecimal.valueOf(0), //annual interest rate=0
                BigDecimal.valueOf(100),
                null,
                LocalDate.now(),
                LoanAccount.RepaymentType.EQUAL_INTEREST
        );

        accountRepository.save(loanAccount);

        mockMvc.perform(put("/accounts/" + loanAccount.getId() + "/debit")
                        .param("amount", "10")
                        .principal(() -> "Alice"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cannot debit a loan account"));
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testRepayDebt() throws Exception{
        //test repayDebt CreditAccount
        Account creditAccount = new CreditAccount("account2",
                BigDecimal.valueOf(1000),
                testUser,
                null,
                true,
                true,
                BigDecimal.valueOf(100), //credit limit
                BigDecimal.valueOf(100), //current debt
                null,
                null,
                AccountType.CREDIT_CARD
        );
        accountRepository.save(creditAccount);

        Account fromAccount = new BasicAccount("from account",
                BigDecimal.valueOf(200),
                null,
                true,
                true,
                AccountType.CASH,
                AccountCategory.FUNDS,
                testUser);
        accountRepository.save(fromAccount);

        mockMvc.perform(put("/accounts/" + creditAccount.getId() + "/repay-debt")
                        .param("amount", "50")
                        .param("fromAccountId", fromAccount.getId().toString())
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Debt repaid successfully"));

        Account updateCreditAccount=accountRepository.findByName("account2");
        Assertions.assertNotNull(updateCreditAccount);
        Assertions.assertEquals(new BigDecimal("50"), ((CreditAccount)updateCreditAccount).getCurrentDebt());

        Account updateFromAccount=accountRepository.findByName("from account");
        Assertions.assertNotNull(updateFromAccount);
        Assertions.assertEquals(new BigDecimal("150"), updateFromAccount.getBalance());

        User updateUser=userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(new BigDecimal("1150"), updateUser.getTotalAssets());
        Assertions.assertEquals(new BigDecimal("50"), updateUser.getTotalLiabilities());
        Assertions.assertEquals(new BigDecimal("1100"), updateUser.getNetAssets());
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testRepayInstallmentPlan() throws Exception{
        //test repayInstallmentPlan CreditAccount
        Account creditAccount = new CreditAccount("account2",
                BigDecimal.valueOf(1000),
                testUser,
                null,
                true,
                true,
                BigDecimal.valueOf(1000), //credit limit
                BigDecimal.valueOf(0), //current debt
                null,
                null,
                AccountType.CREDIT_CARD
        );
        accountRepository.save(creditAccount);

        InstallmentPlan installmentPlan = new InstallmentPlan(
                BigDecimal.valueOf(1200), // total amount
                12, // total periods
                BigDecimal.valueOf(0), // fee rate
                1, // repaid periods
                InstallmentPlan.FeeStrategy.EVENLY_SPLIT,
                creditAccount // linked account
        );
        installmentPlanRepository.save(installmentPlan);

        ((CreditAccount) creditAccount).addInstallmentPlan(installmentPlan);
        accountRepository.save(creditAccount);


        mockMvc.perform(put("/accounts/" + creditAccount.getId() + "/repay-installment-plan")
                        .param("installmentPlanId", installmentPlan.getId().toString())
                        .principal(() -> "Alice")
                        .param("amount", ""))
                .andExpect(status().isOk())
                .andExpect(content().string("Installment plan repaid successfully"));

        Account updateCreditAccount=accountRepository.findByName("account2");
        Assertions.assertEquals(new BigDecimal("1000.00"), ((CreditAccount)updateCreditAccount).getCurrentDebt());
        Assertions.assertEquals(2, ((CreditAccount)updateCreditAccount).getInstallmentPlans().get(0).getPaidPeriods());

        InstallmentPlan updateInstallmentPlan=installmentPlanRepository.findById(installmentPlan.getId()).orElse(null);
        Assertions.assertEquals(2, updateInstallmentPlan.getPaidPeriods());

        User updateUser=userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(new BigDecimal("900.00"), updateUser.getTotalAssets());
        Assertions.assertEquals(new BigDecimal("1000.00"), updateUser.getTotalLiabilities());
        Assertions.assertEquals(new BigDecimal("-100.00"), updateUser.getNetAssets());

        //repayInstallmentPlan with specific amount
        mockMvc.perform(put("/accounts/" + creditAccount.getId() + "/repay-installment-plan")
                        .param("installmentPlanId", installmentPlan.getId().toString())
                        .principal(() -> "Alice")
                        .param("amount", "200"))
                .andExpect(status().isOk())
                .andExpect(content().string("Installment plan repaid successfully"));

        Account updateCreditAccount2=accountRepository.findByName("account2");
        Assertions.assertEquals(new BigDecimal("800.00"), ((CreditAccount)updateCreditAccount2).getCurrentDebt());
        Assertions.assertEquals(4, ((CreditAccount)updateCreditAccount2).getInstallmentPlans().get(0).getPaidPeriods());

        InstallmentPlan updateInstallmentPlan2=installmentPlanRepository.findById(installmentPlan.getId()).orElse(null);
        Assertions.assertEquals(4, updateInstallmentPlan2.getPaidPeriods());

        User updateUser2=userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(new BigDecimal("700.00"), updateUser2.getTotalAssets());
        Assertions.assertEquals(new BigDecimal("800.00"), updateUser2.getTotalLiabilities());
        Assertions.assertEquals(new BigDecimal("-100.00"), updateUser2.getNetAssets());
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testRepayLoan() throws Exception{
        //test repayLoan LoanAccount
        Account loanAccount = new LoanAccount("account2",
                testUser,
                null,
                true,
                36,
                1,
                BigDecimal.valueOf(1), //annual interest rate
                BigDecimal.valueOf(100), //loan amount
                null, //receiving account
                LocalDate.now(),
                LoanAccount.RepaymentType.EQUAL_INTEREST
        );
        accountRepository.save(loanAccount);

        mockMvc.perform(put("/accounts/" + loanAccount.getId() + "/repay-loan")
                        .principal(() -> "Alice")
                        .param("fromAccountId", "")
                        .param("amount", ""))
                .andExpect(status().isOk())
                .andExpect(content().string("Loan repaid successfully"));

        Account updateLoanAccount=accountRepository.findByName("account2");
        Assertions.assertEquals(2, ((LoanAccount)updateLoanAccount).getRepaidPeriods());
        Assertions.assertEquals(new BigDecimal("95.88"), ((LoanAccount)updateLoanAccount).getRemainingLoanAmount());

        User updateUser=userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(new BigDecimal("0"), updateUser.getTotalAssets());
        Assertions.assertEquals(new BigDecimal("95.88"), updateUser.getTotalLiabilities());
        Assertions.assertEquals(new BigDecimal("-95.88"), updateUser.getNetAssets());

        mockMvc.perform(put("/accounts/" + loanAccount.getId() + "/repay-loan")
                        .principal(() -> "Alice")
                        .param("fromAccountId", "")
                        .param("amount", "28.5"))
                .andExpect(status().isOk())
                .andExpect(content().string("Loan repaid successfully"));

        Account updateLoanAccount2=accountRepository.findByName("account2");
        Assertions.assertEquals(12, ((LoanAccount)updateLoanAccount2).getRepaidPeriods());
        Assertions.assertEquals(new BigDecimal("67.68"), ((LoanAccount)updateLoanAccount2).getRemainingLoanAmount());

        User updateUser2=userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(new BigDecimal("0"), updateUser2.getTotalAssets());
        Assertions.assertEquals(new BigDecimal("67.68"), updateUser2.getTotalLiabilities());
        Assertions.assertEquals(new BigDecimal("-67.68"), updateUser2.getNetAssets());

    }
}

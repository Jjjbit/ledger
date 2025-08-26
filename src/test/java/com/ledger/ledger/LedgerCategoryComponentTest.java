package com.ledger.ledger;

import com.ledger.ledger.domain.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest(classes = com.ledger.ledger.LedgerApplication.class)
@Transactional
@AutoConfigureMockMvc
@Rollback
public class LedgerCategoryComponentTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LedgerCategoryComponentRepository ledgerCategoryComponentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private Ledger testLedger1;
    private Ledger testLedger2;
    private User testUser;
    private Account testAccount;

    @BeforeEach
    public void setUp(){
        testUser=new User("Alice", "pass123", User.Role.USER);
        userRepository.save(testUser);

        testLedger1 = new Ledger("Test Ledger", testUser);
        ledgerRepository.save(testLedger1);
        testLedger2=new Ledger("Test Ledger2", testUser);
        ledgerRepository.save(testLedger2);

        testAccount = new BasicAccount("Test Account",
                BigDecimal.valueOf(1000),
                null,
                true,
                true,
                AccountType.CASH,
                AccountCategory.FUNDS,
                testUser);
        accountRepository.save(testAccount);

    }


    @Test
    @WithMockUser(username = "Alice")
    public void testCreateLedgerCategory() throws Exception{

        mockMvc.perform(post("/ledger-category-components/ledgers/"+ testLedger1.getId()+"/categories")
                .param("name", "Food")
                .principal(() -> "Alice")
                .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(content().string("Category created successfully"));

        mockMvc.perform(post("/ledger-category-components/ledgers/"+ testLedger2.getId()+"/categories")
                        .param("name", "Food")
                        .principal(() -> "Alice")
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(content().string("Category created successfully"));

        LedgerCategoryComponent createdCategory=ledgerCategoryComponentRepository.findByLedgerAndName(testLedger1, "Food");
        Assertions.assertNotNull(createdCategory);

        LedgerCategoryComponent createdCategory2=ledgerCategoryComponentRepository.findByLedgerAndName(testLedger2, "Food");
        Assertions.assertNotNull(createdCategory2);

        Assertions.assertNotEquals(createdCategory.getId(), createdCategory2.getId());

        Ledger updateLedger1=ledgerRepository.findByName(testLedger1.getName());
        Assertions.assertEquals(1, updateLedger1.getCategoryComponents().size());

        Ledger updateLedger2=ledgerRepository.findByName(testLedger2.getName());
        Assertions.assertEquals(1, updateLedger2.getCategoryComponents().size());
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testAddSubCategoryToCategory() throws Exception{
        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(foodCategory);
        LedgerCategoryComponent lunch=new LedgerSubCategory("Lunch", CategoryType.EXPENSE, testLedger1);
        foodCategory.add(lunch);
        testLedger1.addCategoryComponent(lunch);
        ledgerCategoryComponentRepository.save(foodCategory);

        mockMvc.perform(post("/ledger-category-components/ledgers/"+ testLedger1.getId()+"/categories/"+foodCategory.getId()+"/subcategories")
                        .param("name", "Lunch")
                        .principal(() -> "Alice")
                        .param("type", "EXPENSE"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Subcategory already exists in this ledger"));

        mockMvc.perform(post("/ledger-category-components/ledgers/"+ testLedger1.getId()+"/categories/"+lunch.getId()+"/subcategories")
                        .param("name", "Breakfast")
                        .principal(() -> "Alice")
                        .param("type", "EXPENSE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("parent must be category"));

        mockMvc.perform(post("/ledger-category-components/ledgers/"+ testLedger1.getId()+"/categories/"+foodCategory.getId()+"/subcategories")
                        .param("name", "")
                        .principal(() -> "Alice")
                        .param("type", "EXPENSE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("name cannot be null"));

        mockMvc.perform(post("/ledger-category-components/ledgers/"+ testLedger1.getId()+"/categories/"+foodCategory.getId()+"/subcategories")
                        .param("name", "Breakfast")
                        .principal(() -> "Alice")
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(content().string("Subcategory created successfully"));

        LedgerCategoryComponent createdSubCategory=ledgerCategoryComponentRepository.findByLedgerAndName(testLedger1, "Breakfast");
        Assertions.assertNotNull(createdSubCategory);

        LedgerCategoryComponent updateFoodCategory=ledgerCategoryComponentRepository.findByLedgerAndName(testLedger1, "Food");
        Assertions.assertEquals(2, updateFoodCategory.getChildren().size());

        Ledger updateLedger1=ledgerRepository.findByName(testLedger1.getName());
        Assertions.assertEquals(3, updateLedger1.getCategoryComponents().size());
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testDeleteLedgerSubCategoryWithTransactions() throws Exception{
        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(foodCategory);
        ledgerCategoryComponentRepository.save(foodCategory);

        LedgerCategoryComponent lunch=new LedgerSubCategory("Lunch", CategoryType.EXPENSE, testLedger1);
        ledgerCategoryComponentRepository.save(lunch);
        foodCategory.add(lunch);
        testLedger1.addCategoryComponent(lunch);

        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, testAccount, testLedger1, lunch);
        transactionRepository.save(transaction1);
        testAccount.addTransaction(transaction1);
        testLedger1.addTransaction(transaction1);
        lunch.addTransaction(transaction1);

        accountRepository.save(testAccount);
        ledgerRepository.save(testLedger1);
        ledgerCategoryComponentRepository.save(foodCategory);
        ledgerCategoryComponentRepository.save(lunch);


        mockMvc.perform(delete("/ledger-category-components/"+ lunch.getId() +"/delete")
                        .principal(() -> "Alice")
                        .param("deleteTransactions", "true")
                        .param("migrateToCategoryId", ""))
                .andExpect(status().isOk())
                .andExpect(content().string("deleted successfully"));

        Ledger updateLedger = ledgerRepository.findById(testLedger1.getId()).orElseThrow();
        Assertions.assertEquals(0, updateLedger.getTransactions().size());

        LedgerCategoryComponent updateLunch=ledgerCategoryComponentRepository.findByLedgerAndName(updateLedger, "Lunch");
        Assertions.assertNull(updateLunch);

        LedgerCategoryComponent updateFoodCategory=ledgerCategoryComponentRepository.findByLedgerAndName(updateLedger, "Food");
        Assertions.assertEquals(0, updateFoodCategory.getChildren().size());
        Assertions.assertEquals(0, updateFoodCategory.getTransactions().size());


        Account updateAccount=accountRepository.findByName("Test Account");
        Assertions.assertEquals(new BigDecimal("1000"), updateAccount.getBalance());

        User updateUser=userRepository.findByUsername("Alice");
        Assertions.assertEquals(new BigDecimal("1000"), updateUser.getTotalAssets());
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testDeleteLedgerSubCategoryWithoutTransactions() throws Exception{
        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        ledgerCategoryComponentRepository.save(foodCategory);


        LedgerCategoryComponent mealsCategory=new LedgerCategory("Meals", CategoryType.EXPENSE, testLedger1);
        ledgerCategoryComponentRepository.save(mealsCategory);


        LedgerCategoryComponent lunch=new LedgerSubCategory("Lunch", CategoryType.EXPENSE, testLedger1);
        lunch.setParent(foodCategory);
        ledgerCategoryComponentRepository.save(lunch);
        //foodCategory.add(lunch);

        //test delete subcategory with transactions migrated to another Subcategory
        LedgerCategoryComponent snacks=new LedgerSubCategory("Snacks", CategoryType.EXPENSE, testLedger1);
        snacks.setParent(foodCategory);
        ledgerCategoryComponentRepository.save(snacks);
        //foodCategory.add(snacks);


        testLedger1.addCategoryComponent(foodCategory);
        testLedger1.addCategoryComponent(mealsCategory);
        testLedger1.addCategoryComponent(lunch);
        testLedger1.addCategoryComponent(snacks);

        //test delete subcategory with transactions migrated to another category
        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, testAccount, testLedger1, lunch);
        transactionRepository.save(transaction1);
        testAccount.addTransaction(transaction1);
        testLedger1.addTransaction(transaction1);
        lunch.addTransaction(transaction1);

        //test delete subcategory with budget
        Budget budget1=new Budget(BigDecimal.valueOf(100), Budget.Period.MONTHLY, lunch, testUser);
        budgetRepository.save(budget1);
        lunch.addBudget(budget1);

        accountRepository.save(testAccount);
        ledgerRepository.save(testLedger1);
        ledgerCategoryComponentRepository.save(foodCategory);

        mockMvc.perform(delete("/ledger-category-components/"+ lunch.getId() +"/delete")
                        .principal(() -> "Alice")
                        .param("deleteTransactions", "false")
                        .param("migrateToCategoryId", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Must provide migrateToCategoryId if not deleting transactions"));

        mockMvc.perform(delete("/ledger-category-components/"+ lunch.getId() +"/delete")
                        .principal(() -> "Alice")
                        .param("deleteTransactions", "false")
                        .param("migrateToCategoryId", String.valueOf(snacks.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Target category must be a category"));

        mockMvc.perform(delete("/ledger-category-components/"+ lunch.getId() +"/delete")
                        .principal(() -> "Alice")
                        .param("deleteTransactions", "false")
                        .param("migrateToCategoryId", String.valueOf(mealsCategory.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string("deleted successfully"));

        Ledger updateLedger = ledgerRepository.findById(testLedger1.getId()).orElseThrow();
        Assertions.assertEquals(1, updateLedger.getTransactions().size());

        LedgerCategoryComponent updateFoodCategory=ledgerCategoryComponentRepository.findById(foodCategory.getId()).orElse(null);
        Assertions.assertEquals(0, updateFoodCategory.getChildren().size());
        Assertions.assertEquals(0, updateFoodCategory.getTransactions().size());

        LedgerCategoryComponent updateLunch=ledgerCategoryComponentRepository.findById(lunch.getId()).orElse(null);
        Assertions.assertNull(updateLunch);

        LedgerCategoryComponent updateMealsCategory=ledgerCategoryComponentRepository.findById(mealsCategory.getId()).orElse(null);
        Assertions.assertEquals(1, updateMealsCategory.getTransactions().size());

        Account updateAccount=accountRepository.findById(testAccount.getId()).orElse(null);
        Assertions.assertEquals(new BigDecimal("990"), updateAccount.getBalance());

        User updateUser=userRepository.findById(testUser.getId()).orElse(null);
        Assertions.assertEquals(new BigDecimal("990"), updateUser.getTotalAssets());

        Budget updateBudget=budgetRepository.findById(budget1.getId()).orElse(null);
        Assertions.assertNull(updateBudget);

    }

    @Test
    @WithMockUser(username = "Alice")
    public void testDeleteLedgerCategoryWithTransactions() throws Exception{ //cancella anche le transazioni
        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(foodCategory);
        ledgerCategoryComponentRepository.save(foodCategory);

        LedgerCategoryComponent mealsCategory=new LedgerCategory("Meals", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(mealsCategory);
        ledgerCategoryComponentRepository.save(mealsCategory);


        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, testAccount, testLedger1, foodCategory);
        transactionRepository.save(transaction1);
        testAccount.addTransaction(transaction1);
        testLedger1.addTransaction(transaction1);
        foodCategory.addTransaction(transaction1);

        //test delete category with budget
        Budget budget1=new Budget(BigDecimal.valueOf(100), Budget.Period.MONTHLY, foodCategory, testUser);
        foodCategory.addBudget(budget1);
        budgetRepository.save(budget1);

        accountRepository.save(testAccount);
        ledgerRepository.save(testLedger1);
        ledgerCategoryComponentRepository.save(foodCategory);

        mockMvc.perform(delete("/ledger-category-components/"+ foodCategory.getId() +"/delete")
                        .principal(() -> "Alice")
                        .param("deleteTransactions", "true")
                        .param("migrateToCategoryId", ""))
                .andExpect(status().isOk())
                .andExpect(content().string("deleted successfully"));

        Ledger updateLedger = ledgerRepository.findById(testLedger1.getId()).orElseThrow();
        Assertions.assertEquals(0, updateLedger.getTransactions().size());

        LedgerCategoryComponent updateFoodCategory=ledgerCategoryComponentRepository.findByLedgerAndName(updateLedger, "Food");
        Assertions.assertNull(updateFoodCategory);

        User updateUser=userRepository.findByUsername("Alice");
        Assertions.assertEquals(new BigDecimal("1000"), updateUser.getTotalAssets());

        Account updateAccount=accountRepository.findByName("Test Account");
        Assertions.assertEquals(new BigDecimal("1000"), updateAccount.getBalance());

        Optional<Budget> updateBudgetOpt = budgetRepository.findById(budget1.getId());
        Assertions.assertTrue(updateBudgetOpt.isEmpty());
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testDeleteCategoryWithoutTransactions() throws Exception { //non cancella transazioni

        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(foodCategory);
        ledgerCategoryComponentRepository.save(foodCategory);

        LedgerCategoryComponent mealsCategory=new LedgerCategory("Meals", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(mealsCategory);
        ledgerCategoryComponentRepository.save(mealsCategory);


        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, testAccount, testLedger1, foodCategory);
        transactionRepository.save(transaction1);
        testAccount.addTransaction(transaction1);
        testLedger1.addTransaction(transaction1);
        foodCategory.addTransaction(transaction1);

        accountRepository.save(testAccount);
        ledgerRepository.save(testLedger1);
        ledgerCategoryComponentRepository.save(foodCategory);


        mockMvc.perform(delete("/ledger-category-components/"+ foodCategory.getId() +"/delete")
                        .principal(() -> "Alice")
                        .param("deleteTransactions", "false")
                        .param("migrateToCategoryId",  String.valueOf(mealsCategory.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string("deleted successfully"));

        Ledger updateLedger = ledgerRepository.findById(testLedger1.getId()).orElseThrow();
        Assertions.assertEquals(1, updateLedger.getTransactions().size());

        LedgerCategoryComponent updateFoodCategory=ledgerCategoryComponentRepository.findByLedgerAndName(updateLedger, "Food");
        Assertions.assertNull(updateFoodCategory);

        LedgerCategoryComponent updateMealsCategory=ledgerCategoryComponentRepository.findByLedgerAndName(updateLedger, "Meals");
        Assertions.assertEquals(1, updateMealsCategory.getTransactions().size());

        Account updateAccount=accountRepository.findByName("Test Account");
        Assertions.assertEquals(new BigDecimal("990"), updateAccount.getBalance());

        User updateUser=userRepository.findByUsername("Alice");
        Assertions.assertEquals(new BigDecimal("990"), updateUser.getTotalAssets());

    }

    //test delete with budget
    @Test
    @WithMockUser(username = "Alice")
    public void testDeleteCategoryWithSubCategory() throws Exception{
        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(foodCategory);
        ledgerCategoryComponentRepository.save(foodCategory);

        LedgerCategoryComponent lunch=new LedgerSubCategory("Lunch", CategoryType.EXPENSE, testLedger1);
        foodCategory.add(lunch);
        testLedger1.addCategoryComponent(lunch);
        ledgerCategoryComponentRepository.save(lunch);

        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, testAccount, testLedger1, foodCategory);
        transactionRepository.save(transaction1);
        testAccount.addTransaction(transaction1);
        testLedger1.addTransaction(transaction1);
        foodCategory.addTransaction(transaction1);

        accountRepository.save(testAccount);
        ledgerRepository.save(testLedger1);
        ledgerCategoryComponentRepository.save(foodCategory);


        mockMvc.perform(delete("/ledger-category-components/"+ foodCategory.getId() +"/delete")
                        .principal(() -> "Alice")
                        .param("deleteTransactions", "true")
                        .param("migrateToCategoryId", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cannot delete a category that has subcategories"));

    }

    @Test
    @WithMockUser(username = "Alice")
    public void testRename() throws Exception{
        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(foodCategory);
        ledgerCategoryComponentRepository.save(foodCategory);

        mockMvc.perform(put("/ledger-category-components/"+foodCategory.getId()+"/rename")
                        .param("newName", "Food")
                        .principal(() -> "Alice"))
                .andExpect(status().isConflict())
                .andExpect(content().string("new name exists already"));

        mockMvc.perform(put("/ledger-category-components/"+foodCategory.getId() + "/rename")
                .param("newName", "food")
                .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("rename successfully"));

        LedgerCategoryComponent updateCategory=ledgerCategoryComponentRepository.findByLedgerAndName(testLedger1, "food");
        Assertions.assertNotNull(updateCategory);


        mockMvc.perform(put("/ledger-category-components/"+foodCategory.getId() +"/rename")
                        .param("newName", "")
                        .principal(() -> "Alice"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("new name cannot be null"));
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testDemote() throws Exception{
        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(foodCategory);
        ledgerCategoryComponentRepository.save(foodCategory);

        LedgerCategoryComponent mealsCategory=new LedgerCategory("Meals", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(mealsCategory);
        ledgerCategoryComponentRepository.save(mealsCategory);

        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, testAccount, testLedger1, foodCategory);
        transactionRepository.save(transaction1);
        testAccount.addTransaction(transaction1);
        testLedger1.addTransaction(transaction1);
        foodCategory.addTransaction(transaction1);

        accountRepository.save(testAccount);
        ledgerRepository.save(testLedger1);
        ledgerCategoryComponentRepository.save(foodCategory);


        mockMvc.perform(put("/ledger-category-components/"+ foodCategory.getId() +"/demote")
                        .principal(() -> "Alice")
                        .param("parentId", String.valueOf(mealsCategory.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string("Demoted successfully"));

        LedgerCategoryComponent updateFoodCategory=ledgerCategoryComponentRepository.findByLedgerAndName(testLedger1, "Food");
        Assertions.assertEquals(mealsCategory.getId(), updateFoodCategory.getParent().getId());

        LedgerCategoryComponent updateMealsCategory=ledgerCategoryComponentRepository.findByLedgerAndName(testLedger1, "Meals");
        Assertions.assertEquals(1, updateMealsCategory.getChildren().size());
        Assertions.assertEquals(1, updateMealsCategory.getTransactions().size());
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testDemoteWithChildren() throws Exception{
        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(foodCategory);
        ledgerCategoryComponentRepository.save(foodCategory);

        LedgerCategoryComponent lunch=new LedgerSubCategory("Lunch", CategoryType.EXPENSE, testLedger1);
        foodCategory.add(lunch);
        testLedger1.addCategoryComponent(lunch);
        ledgerCategoryComponentRepository.save(lunch);

        LedgerCategoryComponent mealsCategory=new LedgerCategory("Meals", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(mealsCategory);
        ledgerCategoryComponentRepository.save(mealsCategory);

        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, testAccount, testLedger1, foodCategory);
        transactionRepository.save(transaction1);
        testAccount.addTransaction(transaction1);
        testLedger1.addTransaction(transaction1);
        foodCategory.addTransaction(transaction1);

        accountRepository.save(testAccount);
        ledgerRepository.save(testLedger1);
        ledgerCategoryComponentRepository.save(foodCategory);

        mockMvc.perform(put("/ledger-category-components/"+ foodCategory.getId() +"/demote")
                        .principal(() -> "Alice")
                        .param("parentId", String.valueOf(mealsCategory.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cannot demote category with subcategories"));
    }

    @Test
    @WithMockUser(username = "Alice")
    public void testPromote() throws Exception{
        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(foodCategory);
        ledgerCategoryComponentRepository.save(foodCategory);

        LedgerCategoryComponent lunch=new LedgerSubCategory("Lunch", CategoryType.EXPENSE, testLedger1);
        foodCategory.add(lunch);
        testLedger1.addCategoryComponent(lunch);
        ledgerCategoryComponentRepository.save(lunch);

        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, testAccount, testLedger1, foodCategory);
        transactionRepository.save(transaction1);
        testAccount.addTransaction(transaction1);
        testLedger1.addTransaction(transaction1);
        foodCategory.addTransaction(transaction1);

        mockMvc.perform(put("/ledger-category-components/"+ foodCategory.getId() +"/promote")
                        .principal(() -> "Alice"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Must be a SubCategory"));

        mockMvc.perform(put("/ledger-category-components/"+ lunch.getId() +"/promote")
                        .principal(() -> "Alice"))
                .andExpect(status().isOk())
                .andExpect(content().string("Promoted successfully"));

    }

    @Test
    @WithMockUser(username = "Alice")
    public void testChangeParent() throws Exception{
        LedgerCategoryComponent foodCategory=new LedgerCategory("Food", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(foodCategory);
        ledgerCategoryComponentRepository.save(foodCategory);

        LedgerCategoryComponent lunch=new LedgerSubCategory("Lunch", CategoryType.EXPENSE, testLedger1);
        foodCategory.add(lunch);
        testLedger1.addCategoryComponent(lunch);
        ledgerCategoryComponentRepository.save(lunch);

        Transaction transaction1 = new Expense(LocalDate.now(), BigDecimal.valueOf(10),null, testAccount, testLedger1, lunch);
        transactionRepository.save(transaction1);
        testAccount.addTransaction(transaction1);
        testLedger1.addTransaction(transaction1);
        lunch.addTransaction(transaction1);

        LedgerCategoryComponent mealsCategory=new LedgerCategory("Meals", CategoryType.EXPENSE, testLedger1);
        testLedger1.addCategoryComponent(mealsCategory);
        ledgerCategoryComponentRepository.save(mealsCategory);

        accountRepository.save(testAccount);
        ledgerRepository.save(testLedger1);
        ledgerCategoryComponentRepository.save(lunch);

        mockMvc.perform(put("/ledger-category-components/"+ foodCategory.getId() +"/change-parent")
                        .principal(() -> "Alice")
                        .param("newParentId", String.valueOf(mealsCategory.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cannot change parent of category"));

        mockMvc.perform(put("/ledger-category-components/"+ lunch.getId() +"/change-parent")
                        .principal(() -> "Alice")
                        .param("newParentId", String.valueOf(mealsCategory.getId())))
                .andExpect(status().isOk())
                .andExpect(content().string("Changed parent successfully"));

        LedgerCategoryComponent updateLunch=ledgerCategoryComponentRepository.findByLedgerAndName(testLedger1, "Lunch");
        Assertions.assertEquals(mealsCategory.getId(), updateLunch.getParent().getId());

        LedgerCategoryComponent updateMealsCategory=ledgerCategoryComponentRepository.findByLedgerAndName(testLedger1, "Meals");
        Assertions.assertEquals(1, updateMealsCategory.getChildren().size());
        Assertions.assertEquals(1, updateMealsCategory.getTransactions().size());

        LedgerCategoryComponent updateFoodCategory=ledgerCategoryComponentRepository.findByLedgerAndName(testLedger1, "Food");
        Assertions.assertEquals(0, updateFoodCategory.getChildren().size());

    }

}

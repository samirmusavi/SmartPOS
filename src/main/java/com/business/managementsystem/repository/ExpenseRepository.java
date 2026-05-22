package com.business.managementsystem.repository;

import com.business.managementsystem.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ── Global (owner view — no branch filter) ────────────────────
    List<Expense> findByBusinessIdOrderByExpenseDateDesc(Long businessId);

    List<Expense> findByBusinessIdAndRecurringTrue(Long businessId);

    List<Expense> findByBusinessIdAndRecurringFalseOrderByExpenseDateDesc(Long businessId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.businessId = :businessId AND e.recurring = false")
    BigDecimal getTotalOneTimeExpenses(Long businessId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.businessId = :businessId AND e.recurring = true " +
            "AND e.frequency = 'MONTHLY'")
    BigDecimal getTotalMonthlyRecurring(Long businessId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.businessId = :businessId AND e.recurring = true " +
            "AND e.frequency = 'YEARLY'")
    BigDecimal getTotalYearlyRecurring(Long businessId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.businessId = :businessId AND e.recurring = true " +
            "AND e.frequency = 'WEEKLY'")
    BigDecimal getTotalWeeklyRecurring(Long businessId);

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) " +
            "FROM Expense e WHERE e.businessId = :businessId " +
            "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> getExpensesByCategoryByBusiness(Long businessId);

    @Query("SELECT DISTINCT e.category FROM Expense e " +
            "WHERE e.businessId = :businessId ORDER BY e.category")
    List<String> getDistinctCategoriesByBusiness(Long businessId);

    // ── Branch-specific ───────────────────────────────────────────
    List<Expense> findByBusinessIdAndBranchIdOrderByExpenseDateDesc(
            Long businessId, Long branchId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.businessId = :businessId AND e.branchId = :branchId " +
            "AND e.recurring = false")
    BigDecimal getTotalOneTimeExpensesByBranch(Long businessId, Long branchId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.businessId = :businessId AND e.branchId = :branchId " +
            "AND e.recurring = true AND e.frequency = 'MONTHLY'")
    BigDecimal getTotalMonthlyRecurringByBranch(Long businessId, Long branchId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.businessId = :businessId AND e.branchId = :branchId " +
            "AND e.recurring = true AND e.frequency = 'YEARLY'")
    BigDecimal getTotalYearlyRecurringByBranch(Long businessId, Long branchId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
            "WHERE e.businessId = :businessId AND e.branchId = :branchId " +
            "AND e.recurring = true AND e.frequency = 'WEEKLY'")
    BigDecimal getTotalWeeklyRecurringByBranch(Long businessId, Long branchId);

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) " +
            "FROM Expense e WHERE e.businessId = :businessId " +
            "AND e.branchId = :branchId " +
            "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> getExpensesByCategoryByBranch(Long businessId, Long branchId);

    // ── Cash Sheet: one-time expenses on a specific date ──────────
    List<Expense> findByBusinessIdAndBranchIdAndExpenseDateAndRecurringFalse(
            Long businessId, Long branchId, java.time.LocalDate expenseDate);
}
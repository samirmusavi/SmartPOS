package com.business.managementsystem.service;

import com.business.managementsystem.model.Expense;
import com.business.managementsystem.repository.BranchRepository;
import com.business.managementsystem.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BranchRepository branchRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          BranchRepository branchRepository) {
        this.expenseRepository = expenseRepository;
        this.branchRepository = branchRepository;
    }

    // ── Get all expenses — branch-aware ──────────────────────────
    @Transactional(readOnly = true)
    public List<Expense> getAllExpenses(Long businessId, Long branchId) {
        if (branchId != null) {
            return expenseRepository
                    .findByBusinessIdAndBranchIdOrderByExpenseDateDesc(
                            businessId, branchId);
        }
        return expenseRepository
                .findByBusinessIdOrderByExpenseDateDesc(businessId);
    }

    // ── Create expense — saves branchId on record ─────────────────
    @Transactional
    public Expense createExpense(Long businessId, Long branchId,
                                 String title, String description,
                                 BigDecimal amount, String category,
                                 LocalDate expenseDate, boolean recurring,
                                 Expense.Frequency frequency,
                                 LocalDate recurringEndDate) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Amount must be greater than zero.");
        if (recurring && frequency == null)
            throw new RuntimeException("Frequency is required for recurring expenses.");

        Expense expense = new Expense(
                businessId, title, description, amount, category,
                expenseDate, recurring, frequency, recurringEndDate
        );
        if (branchId != null) expense.setBranchId(branchId);
        return expenseRepository.save(expense);
    }

    // ── Update expense ────────────────────────────────────────────
    @Transactional
    public Expense updateExpense(Long id, Long businessId, String title,
                                 String description, BigDecimal amount,
                                 String category, LocalDate expenseDate,
                                 boolean recurring,
                                 Expense.Frequency frequency,
                                 LocalDate recurringEndDate) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found."));

        if (!expense.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Amount must be greater than zero.");
        if (recurring && frequency == null)
            throw new RuntimeException("Frequency is required for recurring expenses.");

        expense.setTitle(title);
        expense.setDescription(description);
        expense.setAmount(amount);
        expense.setCategory(category);
        expense.setExpenseDate(expenseDate);
        expense.setRecurring(recurring);
        expense.setFrequency(recurring ? frequency : null);
        expense.setRecurringEndDate(recurring ? recurringEndDate : null);
        return expenseRepository.save(expense);
    }

    // ── Delete expense ────────────────────────────────────────────
    @Transactional
    public void deleteExpense(Long id, Long businessId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found."));
        if (!expense.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");
        expenseRepository.delete(expense);
    }

    // ── Summary — branch-aware ────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long businessId, Long branchId) {
        List<Expense> all = branchId != null
                ? expenseRepository.findByBusinessIdAndBranchIdOrderByExpenseDateDesc(
                businessId, branchId)
                : expenseRepository.findByBusinessIdOrderByExpenseDateDesc(businessId);

        BigDecimal totalOneTime = BigDecimal.ZERO;
        BigDecimal monthlyFixed = BigDecimal.ZERO;
        BigDecimal weeklyFixed  = BigDecimal.ZERO;
        BigDecimal yearlyFixed  = BigDecimal.ZERO;

        LocalDate today = LocalDate.now();

        for (Expense e : all) {
            if (e.isRecurring() && e.getRecurringEndDate() != null
                    && e.getRecurringEndDate().isBefore(today)) {
                continue;
            }

            if (!e.isRecurring()) {
                totalOneTime = totalOneTime.add(e.getAmount());
            } else if (e.getFrequency() == Expense.Frequency.MONTHLY) {
                monthlyFixed = monthlyFixed.add(e.getAmount());
            } else if (e.getFrequency() == Expense.Frequency.WEEKLY) {
                weeklyFixed = weeklyFixed.add(e.getAmount());
            } else if (e.getFrequency() == Expense.Frequency.YEARLY) {
                yearlyFixed = yearlyFixed.add(e.getAmount());
            }
        }

        BigDecimal weeklyAsMonthly  = weeklyFixed
                .multiply(new BigDecimal("4.33"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal yearlyAsMonthly  = yearlyFixed
                .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        BigDecimal totalMonthlyBurden = monthlyFixed
                .add(weeklyAsMonthly)
                .add(yearlyAsMonthly);

        // Category breakdown — built from the already-fetched list
        Map<String, BigDecimal> catTotals = new HashMap<>();
        for (Expense e : all) {
            catTotals.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }
        List<Map<String, Object>> categories = catTotals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(entry -> {
                    Map<String, Object> cat = new HashMap<>();
                    cat.put("category", entry.getKey());
                    cat.put("total",    entry.getValue());
                    return cat;
                })
                .toList();

        // Recurring list (active only) — includes branchName for global view
        List<Map<String, Object>> recurringList = all.stream()
                .filter(Expense::isRecurring)
                .filter(e -> e.getRecurringEndDate() == null
                        || !e.getRecurringEndDate().isBefore(today))
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",               e.getId());
                    m.put("title",            e.getTitle());
                    m.put("amount",           e.getAmount());
                    m.put("category",         e.getCategory());
                    m.put("frequency",        e.getFrequency().name());
                    m.put("recurringEndDate", e.getRecurringEndDate() != null
                            ? e.getRecurringEndDate().toString() : null);
                    m.put("active",           true);
                    m.put("branchId",         e.getBranchId());
                    // Resolve branch name for display
                    if (e.getBranchId() != null) {
                        m.put("branchName", branchRepository.findById(e.getBranchId())
                                .map(b -> b.getName()).orElse(null));
                    } else {
                        m.put("branchName", null);
                    }
                    return m;
                }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("totalOneTimeExpenses",    totalOneTime);
        result.put("monthlyRecurringTotal",   monthlyFixed);
        result.put("weeklyRecurringTotal",    weeklyFixed);
        result.put("yearlyRecurringTotal",    yearlyFixed);
        result.put("totalMonthlyBurden",      totalMonthlyBurden);
        result.put("expenseByCategory",       categories);
        result.put("recurringExpenses",       recurringList);
        return result;
    }

    // Legacy overload — kept so any existing callers don't break
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long businessId) {
        return getSummary(businessId, null);
    }

    // Legacy overload for getMonthlySummary (referenced in handoff doc)
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlySummary(Long businessId) {
        return getSummary(businessId, null);
    }
}
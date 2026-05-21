package com.business.managementsystem.controller;

import com.business.managementsystem.model.Expense;
import com.business.managementsystem.repository.BranchRepository;
import com.business.managementsystem.service.ExpenseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final BranchRepository branchRepository;

    public ExpenseController(ExpenseService expenseService,
                             BranchRepository branchRepository) {
        this.expenseService = expenseService;
        this.branchRepository = branchRepository;
    }

    private Long getBusinessId(String header) {
        if (header == null || header.isBlank())
            throw new RuntimeException("Business ID header is required.");
        return Long.parseLong(header);
    }

    private Long parseBranchId(String header) {
        if (header == null || header.isBlank()) return null;
        return Long.parseLong(header);
    }

    private Map<String, Object> toMap(Expense e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",               e.getId());
        m.put("branchId",         e.getBranchId());
        // Resolve branch name for display in global view
        if (e.getBranchId() != null) {
            String bName = branchRepository.findById(e.getBranchId())
                    .map(b -> b.getName()).orElse(null);
            m.put("branchName", bName);
        } else {
            m.put("branchName", null);
        }
        m.put("title",            e.getTitle());
        m.put("description",      e.getDescription());
        m.put("amount",           e.getAmount());
        m.put("category",         e.getCategory());
        m.put("expenseDate",      e.getExpenseDate() != null
                ? e.getExpenseDate().toString() : null);
        m.put("recurring",        e.isRecurring());
        m.put("frequency",        e.getFrequency() != null
                ? e.getFrequency().name() : null);
        m.put("recurringEndDate", e.getRecurringEndDate() != null
                ? e.getRecurringEndDate().toString() : null);
        m.put("createdAt",        e.getCreatedAt() != null
                ? e.getCreatedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }

    // GET /api/expenses — branch-aware list
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                expenseService.getAllExpenses(getBusinessId(h), parseBranchId(brh))
                        .stream().map(this::toMap).toList()
        );
    }

    // GET /api/expenses/summary — branch-aware stat cards
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh) {
        return ResponseEntity.ok(
                expenseService.getSummary(getBusinessId(h), parseBranchId(brh))
        );
    }

    // POST /api/expenses — saves branchId from header onto the record
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestHeader(value = "X-Branch-Id",   required = false) String brh,
            @RequestBody Map<String, String> request) {

        boolean recurring = "true".equals(request.get("recurring"));
        Expense.Frequency frequency = null;
        if (recurring && request.get("frequency") != null) {
            frequency = Expense.Frequency.valueOf(request.get("frequency"));
        }
        LocalDate endDate = null;
        if (request.get("recurringEndDate") != null
                && !request.get("recurringEndDate").isBlank()) {
            endDate = LocalDate.parse(request.get("recurringEndDate"));
        }

        Expense expense = expenseService.createExpense(
                getBusinessId(h),
                parseBranchId(brh),
                request.get("title"),
                request.get("description"),
                new BigDecimal(request.get("amount")),
                request.get("category"),
                LocalDate.parse(request.get("expenseDate")),
                recurring,
                frequency,
                endDate
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(expense));
    }

    // PUT /api/expenses/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h,
            @RequestBody Map<String, String> request) {

        boolean recurring = "true".equals(request.get("recurring"));
        Expense.Frequency frequency = null;
        if (recurring && request.get("frequency") != null) {
            frequency = Expense.Frequency.valueOf(request.get("frequency"));
        }
        LocalDate endDate = null;
        if (request.get("recurringEndDate") != null
                && !request.get("recurringEndDate").isBlank()) {
            endDate = LocalDate.parse(request.get("recurringEndDate"));
        }

        Expense expense = expenseService.updateExpense(
                id,
                getBusinessId(h),
                request.get("title"),
                request.get("description"),
                new BigDecimal(request.get("amount")),
                request.get("category"),
                LocalDate.parse(request.get("expenseDate")),
                recurring,
                frequency,
                endDate
        );
        return ResponseEntity.ok(toMap(expense));
    }

    // DELETE /api/expenses/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Business-Id", required = false) String h) {
        expenseService.deleteExpense(id, getBusinessId(h));
        return ResponseEntity.noContent().build();
    }
}
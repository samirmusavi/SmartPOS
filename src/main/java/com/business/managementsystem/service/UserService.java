package com.business.managementsystem.service;

import com.business.managementsystem.model.Branch;
import com.business.managementsystem.model.Business;
import com.business.managementsystem.model.User;
import com.business.managementsystem.model.UserBranch;
import com.business.managementsystem.repository.BranchRepository;
import com.business.managementsystem.repository.BusinessRepository;
import com.business.managementsystem.repository.UserBranchRepository;
import com.business.managementsystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository       userRepository;
    private final BusinessRepository   businessRepository;
    private final BranchRepository     branchRepository;
    private final UserBranchRepository userBranchRepository;
    private final PasswordEncoder      passwordEncoder;

    public UserService(UserRepository userRepository,
                       BusinessRepository businessRepository,
                       BranchRepository branchRepository,
                       UserBranchRepository userBranchRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository       = userRepository;
        this.businessRepository   = businessRepository;
        this.branchRepository     = branchRepository;
        this.userBranchRepository = userBranchRepository;
        this.passwordEncoder      = passwordEncoder;
    }

    // ── Get all staff for a business — includes branch assignments ──
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStaffByBusiness(Long businessId) {
        List<User> users = userRepository.findByBusinessId(businessId);

        // Load all branches for this business once — for name lookups
        List<Branch> allBranches = branchRepository
                .findByBusinessIdOrderByIsMainBranchDescNameAsc(businessId);
        Map<Long, String> branchNameMap = allBranches.stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName));

        return users.stream().map(u -> {
            Map<String, Object> m = toMap(u);

            // Attach assigned branches
            List<Map<String, Object>> assignedBranches =
                    userBranchRepository.findByUserId(u.getId())
                            .stream()
                            .map(ub -> {
                                Map<String, Object> b = new HashMap<>();
                                b.put("branchId",   ub.getBranchId());
                                b.put("branchName",
                                        branchNameMap.getOrDefault(
                                                ub.getBranchId(), "Unknown"));
                                return b;
                            })
                            .toList();

            m.put("assignedBranches", assignedBranches);
            return m;
        }).toList();
    }

    // ── Add a new staff member ────────────────────────────────────
    @Transactional
    public Map<String, Object> addStaff(Long businessId,
                                        String fullName,
                                        String email,
                                        String password,
                                        User.Role role,
                                        List<Long> branchIds) {
        if (userRepository.existsByEmail(email))
            throw new RuntimeException(
                    "An account with this email already exists.");

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() ->
                        new RuntimeException("Business not found."));

        long currentCount = userRepository.countByBusinessId(businessId);
        int  limit        = getStaffLimit(business.getPlan());
        if (currentCount >= limit)
            throw new RuntimeException(
                    "Staff limit reached for your plan (" + limit +
                            " members). Please upgrade to add more staff.");

        if (role == User.Role.OWNER)
            throw new RuntimeException(
                    "Cannot add another Owner. " +
                            "A business can only have one Owner.");

        User user = new User(
                business, fullName, email,
                passwordEncoder.encode(password), role);
        User saved = userRepository.save(user);

        // Assign to branches
        if (branchIds != null && !branchIds.isEmpty()) {
            assignBranches(saved.getId(), businessId, branchIds);
        }

        Map<String, Object> result = toMap(saved);
        result.put("assignedBranches", getBranchList(saved.getId()));
        return result;
    }

    // ── Update staff status ───────────────────────────────────────
    @Transactional
    public Map<String, Object> updateStaffStatus(Long userId,
                                                 User.Status status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("Staff member not found."));
        if (user.getRole() == User.Role.OWNER)
            throw new RuntimeException(
                    "Cannot deactivate the business Owner.");
        user.setStatus(status);
        Map<String, Object> result = toMap(userRepository.save(user));
        result.put("assignedBranches", getBranchList(userId));
        return result;
    }

    // ── Update staff role ─────────────────────────────────────────
    @Transactional
    public Map<String, Object> updateStaffRole(Long userId,
                                               User.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("Staff member not found."));
        if (user.getRole() == User.Role.OWNER)
            throw new RuntimeException(
                    "Cannot change the role of the business Owner.");
        if (role == User.Role.OWNER)
            throw new RuntimeException(
                    "Cannot assign the Owner role to a staff member.");
        user.setRole(role);
        Map<String, Object> result = toMap(userRepository.save(user));
        result.put("assignedBranches", getBranchList(userId));
        return result;
    }

    // ── Assign a user to one or more branches ─────────────────────
    // Replaces all existing assignments for this user
    @Transactional
    public Map<String, Object> assignBranchesToUser(Long userId,
                                                    Long businessId,
                                                    List<Long> branchIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("Staff member not found."));

        // Clear existing assignments
        userBranchRepository.deleteByUserId(userId);

        // Assign new branches
        if (branchIds != null && !branchIds.isEmpty()) {
            assignBranches(userId, businessId, branchIds);
        }

        Map<String, Object> result = toMap(user);
        result.put("assignedBranches", getBranchList(userId));
        return result;
    }

    // ── Delete a staff member ─────────────────────────────────────
    @Transactional
    public void deleteStaff(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("Staff member not found."));
        if (user.getRole() == User.Role.OWNER)
            throw new RuntimeException(
                    "Cannot delete the business Owner.");
        // Remove branch assignments first
        userBranchRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    // ── Get staff count ───────────────────────────────────────────
    public long getStaffCount(Long businessId) {
        return userRepository.countByBusinessId(businessId);
    }

    // ── Internal: assign branches with validation ─────────────────
    private void assignBranches(Long userId, Long businessId,
                                List<Long> branchIds) {
        for (Long branchId : branchIds) {
            // Verify branch belongs to this business
            boolean valid = branchRepository
                    .findByIdAndBusinessId(branchId, businessId)
                    .isPresent();
            if (!valid) continue;

            // Skip if already assigned
            if (userBranchRepository
                    .existsByUserIdAndBranchId(userId, branchId))
                continue;

            userBranchRepository.save(new UserBranch(userId, branchId));
        }
    }

    // ── Internal: get branch list for a user ─────────────────────
    private List<Map<String, Object>> getBranchList(Long userId) {
        return userBranchRepository.findByUserId(userId)
                .stream()
                .map(ub -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("branchId", ub.getBranchId());
                    String name = branchRepository
                            .findById(ub.getBranchId())
                            .map(Branch::getName)
                            .orElse("Unknown");
                    m.put("branchName", name);
                    return m;
                })
                .toList();
    }

    // ── Convert User to safe map ──────────────────────────────────
    public Map<String, Object> toMap(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",        u.getId());
        m.put("fullName",  u.getFullName());
        m.put("email",     u.getEmail());
        m.put("role",      u.getRole().name());
        m.put("status",    u.getStatus().name());
        m.put("createdAt", u.getCreatedAt() != null
                ? u.getCreatedAt().toString() : null);
        return m;
    }

    // ── Get user by ID ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }

    // ── Change own password ─────────────────────────────────────────
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8)
            throw new RuntimeException("New password must be at least 8 characters.");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new RuntimeException("Current password is incorrect.");

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ── Plan staff limits ─────────────────────────────────────────
    private int getStaffLimit(Business.Plan plan) {
        return switch (plan) {
            case BASIC      -> 3;
            case BUSINESS   -> 10;
            case ENTERPRISE -> Integer.MAX_VALUE;
        };
    }
}
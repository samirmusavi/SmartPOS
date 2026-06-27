package com.business.managementsystem.service;

import com.business.managementsystem.model.Branch;
import com.business.managementsystem.model.Business;
import com.business.managementsystem.security.JwtUtil;
import com.business.managementsystem.model.User;
import com.business.managementsystem.repository.BranchRepository;
import com.business.managementsystem.repository.BusinessRepository;
import com.business.managementsystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository     userRepository;
    private final BusinessRepository businessRepository;
    private final BranchRepository   branchRepository;
    private final PasswordEncoder    passwordEncoder;
    private final JwtUtil            jwtUtil;

    public AuthService(UserRepository userRepository,
                       BusinessRepository businessRepository,
                       BranchRepository branchRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository     = userRepository;
        this.businessRepository = businessRepository;
        this.branchRepository   = branchRepository;
        this.passwordEncoder    = passwordEncoder;
        this.jwtUtil            = jwtUtil;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> clientLogin(String email, String password) {

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));

        // Check password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Check user is active
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new RuntimeException(
                    "Your account has been deactivated. " +
                            "Please contact your business owner.");
        }

        // Get the business
        Business business = user.getBusiness();

        // Check business is active
        if (business.getStatus() != Business.Status.ACTIVE) {
            throw new RuntimeException(
                    "Your business account is " +
                            business.getStatus().toString().toLowerCase() +
                            ". Please contact SmartPOS support.");
        }

        // Load active branches for this business
        // These are sent to the frontend so branch-select.html can
        // show the correct options without an extra API call
        List<Branch> branches = branchRepository
                .findByBusinessIdAndStatus(
                        business.getId(), Branch.Status.ACTIVE);

        List<Map<String, Object>> branchList = branches.stream()
                .map(b -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",           b.getId());
                    m.put("name",         b.getName());
                    m.put("city",         b.getCity());
                    m.put("country",      b.getCountry());
                    m.put("isMainBranch", b.isMainBranch());
                    return m;
                })
                .toList();

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getId(),
                business.getId(),
                user.getEmail(),
                user.getRole().toString());

        // Build session response (includes token + user data for frontend)
        Map<String, Object> session = new HashMap<>();
        session.put("token",        token);
        session.put("userId",       user.getId());
        session.put("fullName",     user.getFullName());
        session.put("email",        user.getEmail());
        session.put("role",         user.getRole().toString());
        session.put("businessId",   business.getId());
        session.put("businessName", business.getBusinessName());
        session.put("plan",         business.getPlan().toString());
        session.put("branches",     branchList);
        session.put("success",      true);

        return session;
    }

    @Transactional
    public Map<String, Object> registerOwner(
            String businessName, String ownerName,
            String email, String phone,
            Business.Plan plan,
            String ownerEmail, String ownerPassword) {

        // Check business email not taken
        if (businessRepository.existsByEmail(email)) {
            throw new RuntimeException(
                    "A business with this email already exists.");
        }

        // Check user email not taken
        if (userRepository.existsByEmail(ownerEmail)) {
            throw new RuntimeException(
                    "A user with this email already exists.");
        }

        // Create business
        Business business = new Business(
                businessName, ownerName, email, phone, plan);
        businessRepository.save(business);

        // Create owner user
        User owner = new User(
                business,
                ownerName,
                ownerEmail,
                passwordEncoder.encode(ownerPassword),
                User.Role.OWNER
        );
        userRepository.save(owner);

        Map<String, Object> response = new HashMap<>();
        response.put("success",      true);
        response.put("businessName", businessName);
        response.put("message",
                "Account created successfully. You can now log in.");
        return response;
    }
}
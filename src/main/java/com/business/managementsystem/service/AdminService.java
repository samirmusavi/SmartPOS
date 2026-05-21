package com.business.managementsystem.service;

import com.business.managementsystem.model.Admin;
import com.business.managementsystem.model.Business;
import com.business.managementsystem.model.User;
import com.business.managementsystem.repository.AdminRepository;
import com.business.managementsystem.repository.BusinessRepository;
import com.business.managementsystem.repository.SaleRepository;
import com.business.managementsystem.repository.SaleTransactionRepository;
import com.business.managementsystem.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AdminService {

    private final AdminRepository            adminRepository;
    private final BusinessRepository         businessRepository;
    private final UserRepository             userRepository;
    private final SaleRepository             saleRepository;
    private final SaleTransactionRepository  txRepository;
    private final PasswordEncoder            passwordEncoder;
    private final com.business.managementsystem.repository.ProductRepository productRepository;

    // ── Plan pricing (AED/month) ─────────────────────────────
    private static final BigDecimal PRICE_BASIC      = new BigDecimal("99");
    private static final BigDecimal PRICE_BUSINESS   = new BigDecimal("249");
    private static final BigDecimal PRICE_ENTERPRISE  = new BigDecimal("499");

    // ── Health score thresholds ──────────────────────────────
    private static final int HEALTH_AT_RISK_DAYS = 3;

    public AdminService(AdminRepository adminRepository,
                        BusinessRepository businessRepository,
                        UserRepository userRepository,
                        SaleRepository saleRepository,
                        SaleTransactionRepository txRepository,
                        PasswordEncoder passwordEncoder,
                        com.business.managementsystem.repository.ProductRepository productRepository) {
        this.adminRepository    = adminRepository;
        this.businessRepository = businessRepository;
        this.userRepository     = userRepository;
        this.saleRepository     = saleRepository;
        this.txRepository       = txRepository;
        this.passwordEncoder    = passwordEncoder;
        this.productRepository  = productRepository;
    }

    // ── ADMIN LOGIN ──────────────────────────────────────────
    public Admin login(String username, String password) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Invalid username or password"));
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        return admin;
    }

    // ── DEFAULT ADMIN SETUP ──────────────────────────────────
    @Transactional
    public void initializeDefaultAdmin() {
        if (adminRepository.count() == 0) {
            Admin admin = new Admin(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "System Administrator"
            );
            adminRepository.save(admin);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  ENHANCED DASHBOARD STATS
    //  Returns all 7 features for admin-dashboard.html:
    //  1. MRR trend chart data (6 months)
    //  2. Revenue per plan breakdown
    //  3. ARPA (Average Revenue Per Account)
    //  4. Last Active per business
    //  5. Health / At-Risk flags
    //  6. Top 5 businesses by revenue
    //  7. Subscription expiry alerts
    // ══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // ── Basic counts (unchanged) ─────────────────────────
        long totalBiz = businessRepository.count();
        long activeBiz = businessRepository.countByStatus(Business.Status.ACTIVE);

        stats.put("totalBusinesses",     totalBiz);
        stats.put("activeBusinesses",    activeBiz);
        stats.put("inactiveBusinesses",  businessRepository.countByStatus(Business.Status.INACTIVE));
        stats.put("suspendedBusinesses", businessRepository.countByStatus(Business.Status.SUSPENDED));
        stats.put("basicPlanCount",      businessRepository.countByPlan(Business.Plan.BASIC));
        stats.put("businessPlanCount",   businessRepository.countByPlan(Business.Plan.BUSINESS));
        stats.put("enterprisePlanCount", businessRepository.countByPlan(Business.Plan.ENTERPRISE));
        stats.put("totalUsers",          userRepository.count());

        // ── 1. MRR Chart Data (last 6 months) ───────────────
        // For each of the last 6 months, count ACTIVE businesses
        // that joined before the end of that month, and calculate
        // the MRR based on their plan pricing.
        stats.put("mrrTrend", buildMrrTrend());

        // ── 2. Revenue Per Plan Breakdown ─────────────────────
        // Only count ACTIVE businesses for MRR calculation
        long activeBasic      = businessRepository.countByStatusAndPlan(
                Business.Status.ACTIVE, Business.Plan.BASIC);
        long activeBusiness   = businessRepository.countByStatusAndPlan(
                Business.Status.ACTIVE, Business.Plan.BUSINESS);
        long activeEnterprise = businessRepository.countByStatusAndPlan(
                Business.Status.ACTIVE, Business.Plan.ENTERPRISE);

        BigDecimal mrrBasic      = PRICE_BASIC.multiply(BigDecimal.valueOf(activeBasic));
        BigDecimal mrrBusiness   = PRICE_BUSINESS.multiply(BigDecimal.valueOf(activeBusiness));
        BigDecimal mrrEnterprise = PRICE_ENTERPRISE.multiply(BigDecimal.valueOf(activeEnterprise));
        BigDecimal totalMrr      = mrrBasic.add(mrrBusiness).add(mrrEnterprise);

        stats.put("estimatedMonthlyRevenue", totalMrr);
        stats.put("revenuePerPlan", Map.of(
                "basic",      mrrBasic,
                "business",   mrrBusiness,
                "enterprise", mrrEnterprise,
                "total",      totalMrr
        ));

        // ── 3. ARPA (Average Revenue Per Account) ─────────────
        BigDecimal arpa = activeBiz > 0
                ? totalMrr.divide(BigDecimal.valueOf(activeBiz), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        stats.put("arpa", arpa);

        // ── 4. Last Active per business ───────────────────────
        // Build a map: businessId → lastActiveTimestamp
        Map<Long, String> lastActiveMap = new HashMap<>();
        List<Object[]> activityData = txRepository.findLatestActivityPerBusiness();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        for (Object[] row : activityData) {
            Long bizId = ((Number) row[0]).longValue();
            LocalDateTime lastActive = (LocalDateTime) row[1];
            lastActiveMap.put(bizId, lastActive.format(dtf));
        }
        stats.put("lastActiveMap", lastActiveMap);

        // ── 5. Health / At-Risk flags ─────────────────────────
        // A business is "At Risk" if it has NOT had any transaction
        // in the last HEALTH_AT_RISK_DAYS days.
        LocalDateTime healthThreshold = LocalDateTime.now()
                .minusDays(HEALTH_AT_RISK_DAYS);
        Set<Long> recentlyActive = txRepository
                .findActiveBusinessIdsSince(healthThreshold);

        // Build healthMap: businessId → "HEALTHY" or "AT_RISK"
        Map<Long, String> healthMap = new HashMap<>();
        List<Business> allBusinesses = businessRepository.findAll();
        long atRiskCount = 0;
        for (Business b : allBusinesses) {
            if (b.getStatus() != Business.Status.ACTIVE) {
                healthMap.put(b.getId(), "INACTIVE");
            } else if (recentlyActive.contains(b.getId())) {
                healthMap.put(b.getId(), "HEALTHY");
            } else {
                healthMap.put(b.getId(), "AT_RISK");
                atRiskCount++;
            }
        }
        stats.put("healthMap", healthMap);
        stats.put("atRiskCount", atRiskCount);

        // ── 6. Top 5 Businesses by Revenue (last 30 days) ─────
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> topBizData = saleRepository.getTopBusinessesByRevenue(
                thirtyDaysAgo, Pageable.ofSize(5));

        // Enrich with business names
        List<Map<String, Object>> topBusinesses = new ArrayList<>();
        for (Object[] row : topBizData) {
            Long bizId = ((Number) row[0]).longValue();
            BigDecimal revenue = (BigDecimal) row[1];
            String bizName = allBusinesses.stream()
                    .filter(b -> b.getId().equals(bizId))
                    .map(Business::getBusinessName)
                    .findFirst().orElse("Unknown");
            String bizPlan = allBusinesses.stream()
                    .filter(b -> b.getId().equals(bizId))
                    .map(b -> b.getPlan().name())
                    .findFirst().orElse("BASIC");

            Map<String, Object> entry = new HashMap<>();
            entry.put("businessId", bizId);
            entry.put("businessName", bizName);
            entry.put("plan", bizPlan);
            entry.put("revenue", revenue);
            topBusinesses.add(entry);
        }
        stats.put("topBusinesses", topBusinesses);

        // ── 7. Subscription Expiry Alerts ─────────────────────
        // Active businesses whose subscription expires within 7 days
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysFromNow = now.plusDays(7);
        List<Business> expiringBiz = businessRepository
                .findByStatusAndExpiryDateBetween(
                        Business.Status.ACTIVE, now, sevenDaysFromNow);

        List<Map<String, Object>> expiryAlerts = new ArrayList<>();
        for (Business b : expiringBiz) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("id", b.getId());
            alert.put("businessName", b.getBusinessName());
            alert.put("ownerName", b.getOwnerName());
            alert.put("email", b.getEmail());
            alert.put("plan", b.getPlan().name());
            alert.put("expiryDate", b.getExpiryDate() != null
                    ? b.getExpiryDate().format(dtf) : null);
            expiryAlerts.add(alert);
        }
        stats.put("expiryAlerts", expiryAlerts);

        // ── 8. Churn / Upsell Alerts ─────────────────────
        // Churn: Inactive for 14+ days
        List<Map<String, Object>> churnAlerts = new ArrayList<>();
        LocalDateTime churnThreshold = LocalDateTime.now().minusDays(14);
        for (Business b : allBusinesses) {
            String lastActiveStr = lastActiveMap.get(b.getId());
            if (b.getStatus() == Business.Status.ACTIVE && (lastActiveStr == null || LocalDateTime.parse(lastActiveStr, dtf).isBefore(churnThreshold))) {
                Map<String, Object> c = new HashMap<>();
                c.put("id", b.getId());
                c.put("businessName", b.getBusinessName());
                c.put("daysInactive", lastActiveStr == null ? "Never active" : java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.parse(lastActiveStr, dtf), now) + " days");
                churnAlerts.add(c);
            }
        }
        stats.put("churnAlerts", churnAlerts);

        // Upsell Leads: Basic users at >90% of product limit (200 per PRODUCT.md)
        int BASIC_LIMIT = 200;
        List<Map<String, Object>> upsellLeads = new ArrayList<>();
        for (Business b : allBusinesses) {
            if (b.getStatus() == Business.Status.ACTIVE && b.getPlan() == Business.Plan.BASIC) {
                long prodCount = productRepository.countByBusinessIdAndActiveTrue(b.getId());
                if (prodCount >= BASIC_LIMIT * 0.9) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("id", b.getId());
                    u.put("businessName", b.getBusinessName());
                    u.put("usage", prodCount + "/" + BASIC_LIMIT);
                    upsellLeads.add(u);
                }
            }
        }
        stats.put("upsellLeads", upsellLeads);

        return stats;
    }

    // ── MRR Trend builder (6 months) ─────────────────────────
    // For each of the last 6 months, simulate MRR by counting
    // ACTIVE businesses that had joined before the end of that
    // month. Businesses must have status = ACTIVE.
    private List<Map<String, Object>> buildMrrTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDateTime endOfMonth = ym.atEndOfMonth().atTime(23, 59, 59);

            // Get all ACTIVE businesses that joined before end of this month
            List<Business> activeBeforeMonth = businessRepository
                    .findByStatusAndJoinedDateBefore(
                            Business.Status.ACTIVE, endOfMonth);

            long basic = 0, biz = 0, ent = 0;
            for (Business b : activeBeforeMonth) {
                switch (b.getPlan()) {
                    case BASIC      -> basic++;
                    case BUSINESS   -> biz++;
                    case ENTERPRISE -> ent++;
                }
            }

            BigDecimal mrr = PRICE_BASIC.multiply(BigDecimal.valueOf(basic))
                    .add(PRICE_BUSINESS.multiply(BigDecimal.valueOf(biz)))
                    .add(PRICE_ENTERPRISE.multiply(BigDecimal.valueOf(ent)));

            Map<String, Object> point = new HashMap<>();
            point.put("month", ym.atDay(1).format(monthFmt));
            point.put("mrr", mrr);
            point.put("basic", basic);
            point.put("business", biz);
            point.put("enterprise", ent);
            point.put("total", basic + biz + ent);
            trend.add(point);
        }

        return trend;
    }

    // ── BUSINESS MANAGEMENT ──────────────────────────────────
    public List<Business> getAllBusinesses() {
        return businessRepository.findAll();
    }

    public Business getBusinessById(Long id) {
        return businessRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Business not found with id: " + id));
    }

    @Transactional
    public Business createBusiness(String businessName, String ownerName,
                                   String email, String phone,
                                   Business.Plan plan) {
        if (businessRepository.existsByEmail(email)) {
            throw new RuntimeException(
                    "A business with this email already exists");
        }
        Business business = new Business(
                businessName, ownerName, email, phone, plan);
        return businessRepository.save(business);
    }

    @Transactional
    public Business updateBusinessStatus(Long id, Business.Status status) {
        Business business = getBusinessById(id);
        business.setStatus(status);
        return businessRepository.save(business);
    }

    @Transactional
    public Business updateBusinessPlan(Long id, Business.Plan plan) {
        Business business = getBusinessById(id);
        business.setPlan(plan);
        business.setExpiryDate(LocalDateTime.now().plusMonths(1));
        return businessRepository.save(business);
    }

    @Transactional
    public void deleteBusiness(Long id) {
        if (!businessRepository.existsById(id)) {
            throw new RuntimeException("Business not found with id: " + id);
        }
        businessRepository.deleteById(id);
    }

    // ── USER MANAGEMENT ──────────────────────────────────────
    public List<User> getUsersByBusiness(Long businessId) {
        return userRepository.findByBusinessId(businessId);
    }
}
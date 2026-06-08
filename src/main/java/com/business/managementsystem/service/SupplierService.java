package com.business.managementsystem.service;

import com.business.managementsystem.model.KycDocument;
import com.business.managementsystem.model.Product;
import com.business.managementsystem.model.Supplier;
import com.business.managementsystem.repository.KycDocumentRepository;
import com.business.managementsystem.repository.ProductRepository;
import com.business.managementsystem.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository     supplierRepository;
    private final ProductRepository      productRepository;
    private final KycDocumentRepository  kycDocumentRepository;

    @Value("${app.kyc.upload-dir:uploads/kyc}")
    private String uploadDir;

    public SupplierService(SupplierRepository supplierRepository,
                           ProductRepository productRepository,
                           KycDocumentRepository kycDocumentRepository) {
        this.supplierRepository    = supplierRepository;
        this.productRepository     = productRepository;
        this.kycDocumentRepository = kycDocumentRepository;
    }

    // ── Stats ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getStats(Long businessId) {
        Map<String, Object> m = new HashMap<>();
        m.put("total",    supplierRepository.countByBusinessId(businessId));
        m.put("suppliers", supplierRepository.countByBusinessIdAndIsSupplierTrue(businessId));
        m.put("customers", supplierRepository.countByBusinessIdAndIsCustomerTrue(businessId));
        m.put("both",      supplierRepository.countByBusinessIdAndIsCustomerTrueAndIsSupplierTrue(businessId));
        return m;
    }

    // ── Get all parties (used by parties.html — unfiltered) ──────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllParties(Long businessId) {
        return supplierRepository
                .findByBusinessIdOrderByNameAsc(businessId)
                .stream()
                .map(s -> toMap(s, countProducts(s.getId(), businessId)))
                .toList();
    }

    // ── Get suppliers only (isSupplier = true) ───────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllSuppliers(Long businessId) {
        return supplierRepository
                .findByBusinessIdAndIsSupplierTrueOrderByNameAsc(businessId)
                .stream()
                .map(s -> toMap(s, countProducts(s.getId(), businessId)))
                .toList();
    }

    // ── Get customers only (isCustomer = true) — for /api/customers ──
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllCustomers(Long businessId) {
        return supplierRepository
                .findByBusinessIdAndIsCustomerTrueOrderByNameAsc(businessId)
                .stream()
                .map(this::toCustomerMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerMapById(Long id, Long businessId) {
        Supplier s = supplierRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));
        if (!s.isCustomer())
            throw new RuntimeException("Record is not marked as a customer.");
        return toCustomerMap(s);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchCustomerParties(Long businessId, String q) {
        String lower = q == null ? "" : q.toLowerCase();
        return supplierRepository
                .findByBusinessIdAndIsCustomerTrueOrderByNameAsc(businessId)
                .stream()
                .filter(s -> s.getName().toLowerCase().contains(lower)
                        || (s.getPhone() != null && s.getPhone().toLowerCase().contains(lower))
                        || (s.getEmail() != null && s.getEmail().toLowerCase().contains(lower)))
                .map(this::toCustomerMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerSummary(Long businessId) {
        long count = supplierRepository.countByBusinessIdAndIsCustomerTrue(businessId);
        Map<String, Object> m = new HashMap<>();
        m.put("totalCustomers",   count);
        m.put("totalRevenue",     BigDecimal.ZERO);
        m.put("avgSpend",         BigDecimal.ZERO);
        m.put("topCustomer",      null);
        m.put("topCustomerSpend", BigDecimal.ZERO);
        return m;
    }

    @Transactional
    public Map<String, Object> createCustomerParty(Long businessId, String fullName,
                                                    String phone, String email, String notes) {
        if (fullName == null || fullName.isBlank())
            throw new RuntimeException("Customer name is required.");
        Supplier s = new Supplier(businessId, fullName, null, phone, email, null, notes);
        s.setSupplier(false);
        s.setCustomer(true);
        s.setKycStatus("NOT_VERIFIED");
        return toCustomerMap(supplierRepository.save(s));
    }

    @Transactional
    public Map<String, Object> updateCustomerParty(Long id, Long businessId,
                                                    String fullName, String phone,
                                                    String email, String notes) {
        Supplier s = supplierRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));
        if (fullName == null || fullName.isBlank())
            throw new RuntimeException("Customer name is required.");
        s.setName(fullName);
        s.setPhone(phone);
        s.setEmail(email);
        s.setNotes(notes);
        return toCustomerMap(supplierRepository.save(s));
    }

    @Transactional
    public void deleteCustomerParty(Long id, Long businessId) {
        Supplier s = supplierRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Customer not found."));
        if (!s.isCustomer())
            throw new RuntimeException("Record is not marked as a customer.");
        deleteSupplier(id, businessId);
    }

    // ── Get single party ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getSupplierById(Long id, Long businessId) {
        Supplier s = supplierRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Party not found."));
        return toMap(s, countProducts(id, businessId));
    }

    // ── Create party ─────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> createSupplier(Long businessId, String name,
                                              String contactPerson, String phone,
                                              String email, String address,
                                              String notes,
                                              boolean isSupplier, boolean isCustomer,
                                              String kycStatus, String emiratesId,
                                              String passportNumber, String tradeLicenseNumber,
                                              String idExpiryDateStr, String kycNotes) {
        if (name == null || name.isBlank())
            throw new RuntimeException("Party name is required.");

        Supplier supplier = new Supplier(
                businessId, name, contactPerson,
                phone, email, address, notes);
        supplier.setSupplier(isSupplier);
        supplier.setCustomer(isCustomer);
        supplier.setKycStatus(kycStatus != null ? kycStatus : "NOT_VERIFIED");
        supplier.setEmiratesId(emiratesId);
        supplier.setPassportNumber(passportNumber);
        supplier.setTradeLicenseNumber(tradeLicenseNumber);
        supplier.setIdExpiryDate(parseDate(idExpiryDateStr));
        supplier.setKycNotes(kycNotes);
        // Auto-generate party code on creation
        supplier.setPartyCode(generatePartyCode(name, businessId));
        return toMap(supplierRepository.save(supplier), 0);
    }

    // ── Backwards-compatible create (legacy: only name/contact/phone/email/address/notes) ──
    @Transactional
    public Map<String, Object> createSupplier(Long businessId, String name,
                                              String contactPerson, String phone,
                                              String email, String address,
                                              String notes) {
        return createSupplier(businessId, name, contactPerson, phone, email, address,
                notes, true, false, "NOT_VERIFIED",
                null, null, null, null, null);
    }

    // ── Update party ─────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> updateSupplier(Long id, Long businessId,
                                              String name, String contactPerson,
                                              String phone, String email,
                                              String address, String notes,
                                              boolean isSupplier, boolean isCustomer,
                                              String kycStatus, String emiratesId,
                                              String passportNumber, String tradeLicenseNumber,
                                              String idExpiryDateStr, String kycNotes) {
        Supplier supplier = supplierRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        if (name == null || name.isBlank())
            throw new RuntimeException("Party name is required.");

        supplier.setName(name);
        supplier.setContactPerson(contactPerson);
        supplier.setPhone(phone);
        supplier.setEmail(email);
        supplier.setAddress(address);
        supplier.setNotes(notes);
        supplier.setSupplier(isSupplier);
        supplier.setCustomer(isCustomer);
        if (kycStatus != null) supplier.setKycStatus(kycStatus);
        supplier.setEmiratesId(emiratesId);
        supplier.setPassportNumber(passportNumber);
        supplier.setTradeLicenseNumber(tradeLicenseNumber);
        supplier.setIdExpiryDate(parseDate(idExpiryDateStr));
        supplier.setKycNotes(kycNotes);
        return toMap(supplierRepository.save(supplier),
                countProducts(id, businessId));
    }

    // ── Backwards-compatible update (legacy) ────────────────────────
    @Transactional
    public Map<String, Object> updateSupplier(Long id, Long businessId,
                                              String name, String contactPerson,
                                              String phone, String email,
                                              String address, String notes) {
        Supplier supplier = supplierRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        if (name == null || name.isBlank())
            throw new RuntimeException("Party name is required.");

        supplier.setName(name);
        supplier.setContactPerson(contactPerson);
        supplier.setPhone(phone);
        supplier.setEmail(email);
        supplier.setAddress(address);
        supplier.setNotes(notes);
        return toMap(supplierRepository.save(supplier),
                countProducts(id, businessId));
    }

    // ── Delete party ─────────────────────────────────────────────────
    @Transactional
    public void deleteSupplier(Long id, Long businessId) {
        Supplier supplier = supplierRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        // Unlink all products from this party first
        List<Product> linked = productRepository
                .findByBusinessIdAndActiveTrue(businessId)
                .stream()
                .filter(p -> id.equals(p.getSupplierId()))
                .toList();

        linked.forEach(p -> {
            p.setSupplierId(null);
            productRepository.save(p);
        });

        // Delete KYC documents from disk + DB
        List<KycDocument> docs = kycDocumentRepository.findBySupplierIdOrderByUploadedAtDesc(id);
        docs.forEach(doc -> {
            try { Files.deleteIfExists(Paths.get(doc.getFilePath())); } catch (IOException ignored) {}
        });
        kycDocumentRepository.deleteBySupplierId(id);

        supplierRepository.delete(supplier);
    }

    // ── Link a product to a party ────────────────────────────────────
    @Transactional
    public void linkProduct(Long supplierId, Long productId, Long businessId) {
        supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found."));

        if (!product.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");

        product.setSupplierId(supplierId);
        productRepository.save(product);
    }

    // ── Unlink a product from its party ──────────────────────────────
    @Transactional
    public void unlinkProduct(Long productId, Long businessId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found."));

        if (!product.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");

        product.setSupplierId(null);
        productRepository.save(product);
    }

    // ── Get products for a party ──────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductsForSupplier(
            Long supplierId, Long businessId) {

        return productRepository
                .findByBusinessIdAndActiveTrue(businessId)
                .stream()
                .filter(p -> supplierId.equals(p.getSupplierId()))
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",       p.getId());
                    m.put("name",     p.getName());
                    m.put("barcode",  p.getBarcode());
                    m.put("category", p.getCategory());
                    m.put("quantity", p.getQuantity());
                    m.put("price",    p.getPrice());
                    return m;
                })
                .toList();
    }

    // ── Link supplier ↔ customer ─────────────────────────────────────
    @Transactional
    public Map<String, Object> linkCustomer(Long supplierId, Long customerId, Long businessId) {
        Supplier s = supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new RuntimeException("Party not found."));
        s.setLinkedCustomerId(customerId);
        return toMap(supplierRepository.save(s), countProducts(supplierId, businessId));
    }

    @Transactional
    public Map<String, Object> unlinkCustomer(Long supplierId, Long businessId) {
        Supplier s = supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new RuntimeException("Party not found."));
        s.setLinkedCustomerId(null);
        return toMap(supplierRepository.save(s), countProducts(supplierId, businessId));
    }

    // ── KYC Document: upload ──────────────────────────────────────────
    @Transactional
    public Map<String, Object> uploadKycDocument(Long supplierId, Long businessId,
                                                  MultipartFile file, String documentType,
                                                  String uploadedBy) throws IOException {
        supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        if (file == null || file.isEmpty())
            throw new RuntimeException("File is required.");

        String originalName = file.getOriginalFilename();
        String ext          = (originalName != null && originalName.contains("."))
                              ? originalName.substring(originalName.lastIndexOf('.'))
                              : "";
        String storedName   = UUID.randomUUID() + ext;

        Path uploadPath = Paths.get(uploadDir, String.valueOf(supplierId));
        Files.createDirectories(uploadPath);
        Path target = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        KycDocument doc = new KycDocument(
                supplierId,
                documentType != null ? documentType : "OTHER",
                originalName != null ? originalName : storedName,
                target.toAbsolutePath().toString(),
                file.getSize(),
                uploadedBy
        );
        kycDocumentRepository.save(doc);
        return docToMap(doc);
    }

    // ── KYC Document: delete ──────────────────────────────────────────
    @Transactional
    public void deleteKycDocument(Long supplierId, Long docId, Long businessId) {
        supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        KycDocument doc = kycDocumentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found."));

        if (!doc.getSupplierId().equals(supplierId))
            throw new RuntimeException("Document does not belong to this party.");

        try { Files.deleteIfExists(Paths.get(doc.getFilePath())); } catch (IOException ignored) {}
        kycDocumentRepository.delete(doc);
    }

    // ── KYC Document: list ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getKycDocuments(Long supplierId, Long businessId) {
        supplierRepository.findByIdAndBusinessId(supplierId, businessId)
                .orElseThrow(() -> new RuntimeException("Party not found."));

        return kycDocumentRepository
                .findBySupplierIdOrderByUploadedAtDesc(supplierId)
                .stream()
                .map(this::docToMap)
                .toList();
    }

    // ── Helper: count products linked to a party ──────────────────────
    private int countProducts(Long supplierId, Long businessId) {
        return (int) productRepository
                .findByBusinessIdAndActiveTrue(businessId)
                .stream()
                .filter(p -> supplierId.equals(p.getSupplierId()))
                .count();
    }

    // ── Helper: parse date string ──────────────────────────────────────
    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    // ── Convert Supplier to safe map ──────────────────────────────────
    private Map<String, Object> toMap(Supplier s, int productCount) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",                  s.getId());
        m.put("name",                s.getName());
        m.put("contactPerson",       s.getContactPerson());
        m.put("phone",               s.getPhone());
        m.put("email",               s.getEmail());
        m.put("address",             s.getAddress());
        m.put("notes",               s.getNotes());
        m.put("linkedCustomerId",    s.getLinkedCustomerId());
        m.put("productCount",        productCount);
        m.put("isSupplier",          s.isSupplier());
        m.put("isCustomer",          s.isCustomer());
        m.put("kycStatus",           s.getKycStatus() != null ? s.getKycStatus() : "NOT_VERIFIED");
        m.put("emiratesId",          s.getEmiratesId());
        m.put("passportNumber",      s.getPassportNumber());
        m.put("tradeLicenseNumber",  s.getTradeLicenseNumber());
        m.put("idExpiryDate",        s.getIdExpiryDate() != null ? s.getIdExpiryDate().toString() : null);
        m.put("kycNotes",            s.getKycNotes());
        m.put("partyCode",           s.getPartyCode());
        m.put("createdAt",           s.getCreatedAt() != null
                ? s.getCreatedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }

    // ── Convert Supplier to customer-shaped map (for /api/customers) ─
    public Map<String, Object> toCustomerMap(Supplier s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",              s.getId());
        m.put("fullName",        s.getName());
        m.put("phone",           s.getPhone());
        m.put("email",           s.getEmail());
        m.put("notes",           s.getNotes());
        m.put("loyaltyPoints",   0);
        m.put("totalSpent",      BigDecimal.ZERO);
        m.put("visitCount",      0);
        m.put("lastVisitAt",     null);
        // If this party is also a supplier, expose its own id so customers.html shows the Supplier badge
        m.put("linkedSupplierId", s.isSupplier() ? s.getId() : null);
        m.put("partyCode",        s.getPartyCode());
        m.put("createdAt",       s.getCreatedAt() != null
                ? s.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }

    // ── Party Code generation ─────────────────────────────────────────

    /**
     * Generates a unique party code for a new supplier/customer party.
     *
     * Format: {2–3 UPPERCASE INITIALS}{3-digit sequence}
     * e.g. "Ahmed Al Mansouri" → "AM001",  "Suntech Gold" → "SG001"
     *
     * Stopwords (Arabic honorifics and corporate suffixes) are excluded
     * before building initials so "bin", "al", "ltd" etc. are ignored.
     */
    public String generatePartyCode(String name, Long businessId) {
        String prefix = buildPartyCodePrefix(name);
        List<Supplier> existing =
                supplierRepository.findByBusinessIdAndPartyCodeStartingWith(businessId, prefix);
        int max = 0;
        for (Supplier s : existing) {
            String code = s.getPartyCode();
            if (code != null && code.startsWith(prefix)) {
                String numPart = code.substring(prefix.length());
                try {
                    int n = Integer.parseInt(numPart);
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }

    /**
     * Batch: generates party codes for all parties that don't have one yet.
     * Returns the count of newly assigned codes.
     * Safe to call multiple times — skips parties that already have a code.
     */
    @Transactional
    public int generatePartyCodes(Long businessId) {
        List<Supplier> parties = supplierRepository.findByBusinessIdOrderByNameAsc(businessId);
        int count = 0;
        for (Supplier s : parties) {
            if (s.getPartyCode() == null || s.getPartyCode().isBlank()) {
                s.setPartyCode(generatePartyCode(s.getName(), businessId));
                supplierRepository.save(s);
                count++;
            }
        }
        return count;
    }

    private static final Set<String> PARTY_CODE_STOPWORDS = new HashSet<>(Arrays.asList(
            // Arabic honorifics / prepositions
            "al", "el", "ul", "bin", "bint", "ibn", "abu", "um", "abi", "bani",
            // Corporate suffixes
            "trading", "company", "co", "llc", "ltd", "est", "fze", "fzco",
            "group", "international", "int", "enterprises", "enterprise",
            // English filler
            "and", "the", "of", "for", "in"
    ));

    private String buildPartyCodePrefix(String name) {
        if (name == null || name.isBlank()) return "P";

        // Normalise: lowercase, strip non-alpha, split on whitespace
        String[] words = name.trim().toLowerCase()
                .replaceAll("[^a-z\\s]", "").split("\\s+");

        StringBuilder sb = new StringBuilder();
        String firstSignificant = null;
        for (String w : words) {
            if (w.isBlank() || PARTY_CODE_STOPWORDS.contains(w)) continue;
            if (firstSignificant == null) firstSignificant = w;
            sb.append(Character.toUpperCase(w.charAt(0)));
            if (sb.length() >= 3) break;
        }

        // If nothing built (all stopwords), fall through to full-name fallback
        if (sb.length() == 0) {
            String clean = name.replaceAll("[^a-zA-Z]", "");
            return clean.length() >= 2 ? clean.substring(0, 2).toUpperCase()
                                       : clean.toUpperCase();
        }

        // Single-char prefix: pad with second char of first significant word
        if (sb.length() == 1 && firstSignificant != null && firstSignificant.length() >= 2) {
            sb.append(Character.toUpperCase(firstSignificant.charAt(1)));
        }

        return sb.toString().toUpperCase();
    }

    // ── Convert KycDocument to safe map ───────────────────────────────
    private Map<String, Object> docToMap(KycDocument d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",           d.getId());
        m.put("supplierId",   d.getSupplierId());
        m.put("documentType", d.getDocumentType());
        m.put("fileName",     d.getFileName());
        m.put("fileSize",     d.getFileSize());
        m.put("uploadedBy",   d.getUploadedBy());
        m.put("uploadedAt",   d.getUploadedAt() != null
                ? d.getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                : null);
        return m;
    }
}

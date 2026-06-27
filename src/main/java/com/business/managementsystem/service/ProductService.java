package com.business.managementsystem.service;

import com.business.managementsystem.dto.ProductDTO;
import com.business.managementsystem.model.Branch;
import com.business.managementsystem.model.BranchInventory;
import com.business.managementsystem.model.BranchProductArchive;
import com.business.managementsystem.model.Product;
import com.business.managementsystem.model.Supplier;
import com.business.managementsystem.repository.BranchInventoryRepository;
import com.business.managementsystem.repository.BranchProductArchiveRepository;
import com.business.managementsystem.repository.BranchRepository;
import com.business.managementsystem.repository.ProductRepository;
import com.business.managementsystem.repository.SaleRepository;
import com.business.managementsystem.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository              productRepository;
    private final SaleRepository                 saleRepository;
    private final SupplierRepository             supplierRepository;
    private final BranchInventoryRepository      branchInventoryRepository;
    private final BranchRepository               branchRepository;
    private final BranchProductArchiveRepository branchArchiveRepository;

    @Value("${smartpos.upload.dir:./uploads}")
    private String uploadDir;

    public ProductService(ProductRepository productRepository,
                          SaleRepository saleRepository,
                          SupplierRepository supplierRepository,
                          BranchInventoryRepository branchInventoryRepository,
                          BranchRepository branchRepository,
                          BranchProductArchiveRepository branchArchiveRepository) {
        this.productRepository       = productRepository;
        this.saleRepository          = saleRepository;
        this.supplierRepository      = supplierRepository;
        this.branchInventoryRepository = branchInventoryRepository;
        this.branchRepository        = branchRepository;
        this.branchArchiveRepository = branchArchiveRepository;
    }

    // ── Build supplier ID → name map (avoids N+1) ─────────────────────
    private Map<Long, String> buildSupplierNameMap(Long businessId) {
        return supplierRepository
                .findByBusinessIdOrderByNameAsc(businessId)
                .stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName));
    }

    // ── Build productId → branchQty map ───────────────────────────────
    private Map<Long, Double> buildBranchQtyMap(Long branchId) {
        if (branchId == null) return new HashMap<>();
        Map<Long, Double> map = new HashMap<>();
        branchInventoryRepository.findByBranchId(branchId)
                .forEach(inv -> map.put(inv.getProductId(), inv.getQuantity()));
        return map;
    }

    // ── Sum stock across all branches for every product in a business ───
    @Transactional(readOnly = true)
    public Map<Long, Double> getTotalStockAllBranches(Long businessId) {
        Map<Long, Double> result = new HashMap<>();
        branchInventoryRepository.sumQuantityByProductForBusiness(businessId)
                .forEach(row -> {
                    Long productId = ((Number) row[0]).longValue();
                    double total   = ((Number) row[1]).doubleValue();
                    result.put(productId, total);
                });
        return result;
    }

    // ── Get all ACTIVE products ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts(Long businessId, Long branchId) {
        Map<Long, String>  supplierNames = buildSupplierNameMap(businessId);
        Map<Long, Double> branchQtyMap  = buildBranchQtyMap(branchId);

        Set<Long> branchArchivedIds = (branchId != null)
                ? branchArchiveRepository.findArchivedProductIdsByBranchId(branchId)
                : Set.of();

        return productRepository
                .findByBusinessIdAndActiveTrue(businessId)
                .stream()
                .filter(p -> !branchArchivedIds.contains(p.getId()))
                .map(p -> convertToDTO(p, supplierNames, branchQtyMap))
                .collect(Collectors.toList());
    }

    // ── Get ARCHIVED products ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ProductDTO> getArchivedProducts(Long businessId, Long branchId) {
        Map<Long, String> supplierNames = buildSupplierNameMap(businessId);

        if (branchId != null) {
            Set<Long> archivedIds = branchArchiveRepository
                    .findArchivedProductIdsByBranchId(branchId);

            if (archivedIds.isEmpty()) return List.of();

            Map<Long, Double> branchQtyMap = buildBranchQtyMap(branchId);
            List<ProductDTO> result = new ArrayList<>();
            for (Long pid : archivedIds) {
                productRepository.findById(pid).ifPresent(p -> {
                    if (p.getBusinessId().equals(businessId) && p.isActive()) {
                        result.add(convertToDTO(p, supplierNames, branchQtyMap));
                    }
                });
            }
            return result;
        }

        return productRepository
                .findAllArchivedByBusinessId(businessId)
                .stream()
                .map(p -> convertToDTO(p, supplierNames, new HashMap<>()))
                .collect(Collectors.toList());
    }

    // ── Get product by ID ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id, Long branchId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        String supplierName = null;
        if (product.getSupplierId() != null) {
            supplierName = supplierRepository.findById(product.getSupplierId())
                    .map(Supplier::getName).orElse(null);
        }
        double qty = resolveQty(product, branchId);
        return convertToDTO(product, supplierName, qty);
    }

    // ── Get product by barcode ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public ProductDTO getProductByBarcode(String barcode, Long businessId, Long branchId) {
        Product product = productRepository
                .findByBarcodeAndBusinessId(barcode, businessId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + barcode));

        if (branchId != null &&
                branchArchiveRepository.findByBranchIdAndProductId(branchId, product.getId()).isPresent()) {
            throw new RuntimeException("This product is not available at this branch.");
        }

        String supplierName = null;
        if (product.getSupplierId() != null) {
            supplierName = supplierRepository.findById(product.getSupplierId())
                    .map(Supplier::getName).orElse(null);
        }
        double qty = resolveQty(product, branchId);
        return convertToDTO(product, supplierName, qty);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category, Long businessId, Long branchId) {
        Map<Long, String>  supplierNames = buildSupplierNameMap(businessId);
        Map<Long, Double> branchQtyMap  = buildBranchQtyMap(branchId);
        Set<Long> branchArchivedIds = (branchId != null)
                ? branchArchiveRepository.findArchivedProductIdsByBranchId(branchId)
                : Set.of();

        return productRepository
                .findByCategoryAndBusinessIdAndActiveTrue(category, businessId)
                .stream()
                .filter(p -> !branchArchivedIds.contains(p.getId()))
                .map(p -> convertToDTO(p, supplierNames, branchQtyMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> searchProductsByName(String name, Long businessId, Long branchId) {
        Map<Long, String>  supplierNames = buildSupplierNameMap(businessId);
        Map<Long, Double> branchQtyMap  = buildBranchQtyMap(branchId);
        Set<Long> branchArchivedIds = (branchId != null)
                ? branchArchiveRepository.findArchivedProductIdsByBranchId(branchId)
                : Set.of();

        return productRepository
                .findByNameContainingIgnoreCaseAndBusinessIdAndActiveTrue(name, businessId)
                .stream()
                .filter(p -> !branchArchivedIds.contains(p.getId()))
                .map(p -> convertToDTO(p, supplierNames, branchQtyMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getLowStockProducts(int threshold, Long businessId) {
        Map<Long, String> supplierNames = buildSupplierNameMap(businessId);
        return productRepository
                .findByQuantityLessThanAndBusinessIdAndActiveTrue(threshold, businessId)
                .stream()
                .map(p -> convertToDTO(p, supplierNames, new HashMap<>()))
                .collect(Collectors.toList());
    }

    // ── Create product ─────────────────────────────────────────────────
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO, Long businessId, Long branchId) {
        // Only check uniqueness when a barcode is actually provided — blank barcode is allowed
        String incomingBarcode = (productDTO.getBarcode() != null && !productDTO.getBarcode().isBlank())
                ? productDTO.getBarcode().trim() : null;
        if (incomingBarcode != null
                && productRepository.existsByBarcodeAndBusinessId(incomingBarcode, businessId)) {
            throw new RuntimeException("Product with this barcode already exists.");
        }
        productDTO.setBarcode(incomingBarcode); // normalise empty → null before persisting

        double initialQty = productDTO.getQuantity();
        Product product = convertToEntity(productDTO, businessId);
        product.setQuantity(initialQty);
        Product saved = productRepository.save(product);

        List<Branch> branches = branchRepository
                .findByBusinessIdOrderByIsMainBranchDescNameAsc(businessId);
        for (Branch branch : branches) {
            double qty = (branchId != null && branch.getId().equals(branchId)) ? initialQty : 0;
            branchInventoryRepository.save(new BranchInventory(branch.getId(), saved.getId(), qty));
        }

        String supplierName = null;
        if (saved.getSupplierId() != null) {
            supplierName = supplierRepository.findById(saved.getSupplierId())
                    .map(Supplier::getName).orElse(null);
        }
        return convertToDTO(saved, supplierName, initialQty);
    }

    // ── Update product ─────────────────────────────────────────────────
    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        existing.setName(productDTO.getName());
        existing.setPrice(productDTO.getPrice());
        existing.setCostPrice(productDTO.getCostPrice());
        existing.setQuantity(productDTO.getQuantity());
        existing.setCategory(productDTO.getCategory());
        // Normalise blank barcode → null; only check uniqueness when a new non-blank barcode is set
        String updatedBarcode = (productDTO.getBarcode() != null && !productDTO.getBarcode().isBlank())
                ? productDTO.getBarcode().trim() : null;
        if (updatedBarcode != null && !updatedBarcode.equals(existing.getBarcode())
                && productRepository.existsByBarcodeAndBusinessId(updatedBarcode, existing.getBusinessId())) {
            throw new RuntimeException("Product with this barcode already exists.");
        }
        existing.setBarcode(updatedBarcode);
        existing.setSupplierId(productDTO.getSupplierId());
        existing.setScrap(productDTO.isScrap());
        existing.setUnitType(productDTO.getUnitType());       // Fix 1: was missing from update
        existing.setPurity(productDTO.getPurity());           // Fix 1: was missing from update
        existing.setProductClass(productDTO.getProductClass() != null
                ? productDTO.getProductClass() : "JEWELLERY"); // Fix 3
        if (productDTO.getTotalWeightGrams() != null) {
            existing.setTotalWeightGrams(productDTO.getTotalWeightGrams());
        }
        String supplierName = null;
        if (existing.getSupplierId() != null) {
            supplierName = supplierRepository.findById(existing.getSupplierId())
                    .map(Supplier::getName).orElse(null);
        }
        return convertToDTO(productRepository.save(existing), supplierName, existing.getQuantity());
    }

    // ── Delete / archive product ───────────────────────────────────────
    @Transactional
    public Map<String, String> deleteProduct(Long id, Long branchId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (branchId != null) {
            if (branchArchiveRepository.findByBranchIdAndProductId(branchId, id).isPresent()) {
                throw new RuntimeException("Product is already archived at this branch.");
            }
            branchArchiveRepository.save(new BranchProductArchive(branchId, id));
            return Map.of("action", "BRANCH_ARCHIVED");
        }

        boolean hasSales = !saleRepository.findByProductId(id).isEmpty();
        if (hasSales) {
            product.setActive(false);
            productRepository.save(product);
            branchArchiveRepository.deleteAllByProductId(id);
            return Map.of("action", "ARCHIVED");
        } else {
            deleteImageFile(product);
            branchArchiveRepository.deleteAllByProductId(id);
            productRepository.deleteById(id);
            return Map.of("action", "DELETED");
        }
    }

    // ── Restore product ────────────────────────────────────────────────
    @Transactional
    public ProductDTO restoreProduct(Long id, Long branchId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        if (branchId != null) {
            branchArchiveRepository.deleteByBranchIdAndProductId(branchId, id);
        } else {
            if (product.isActive()) throw new RuntimeException("Product is not archived.");
            product.setActive(true);
            productRepository.save(product);
        }

        String supplierName = null;
        if (product.getSupplierId() != null) {
            supplierName = supplierRepository.findById(product.getSupplierId())
                    .map(Supplier::getName).orElse(null);
        }
        double qty = resolveQty(product, branchId);
        return convertToDTO(product, supplierName, qty);
    }

    // ── Product Image Upload ──────────────────────────────────────────
    @Transactional
    public String saveProductImage(Long productId, Long businessId, MultipartFile file) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (!product.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new RuntimeException("Only image files are allowed.");

        String originalName = file.getOriginalFilename();
        String ext = "jpg";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }

        Path dir = Paths.get(uploadDir, "products", String.valueOf(businessId));
        Files.createDirectories(dir);

        String filename = productId + "_" + System.currentTimeMillis() + "." + ext;
        Path filePath = dir.resolve(filename);

        deleteImageFile(product);
        file.transferTo(filePath.toAbsolutePath().toFile());

        String urlPath = "/uploads/products/" + businessId + "/" + filename;
        product.setImagePath(urlPath);
        productRepository.save(product);
        return urlPath;
    }

    // ── Delete product image ──────────────────────────────────────────
    @Transactional
    public void deleteProductImage(Long productId, Long businessId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        if (!product.getBusinessId().equals(businessId))
            throw new RuntimeException("Unauthorized.");
        deleteImageFile(product);
        product.setImagePath(null);
        productRepository.save(product);
    }

    // ══════════════════════════════════════════════════════════════════
    // CSV / EXCEL IMPORT
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public Map<String, Object> importProductsFromCsv(Long businessId, Long branchId,
                                                     MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        boolean isExcel = filename != null &&
                (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls"));

        List<String[]> rows;
        if (isExcel) {
            rows = parseExcelFile(file);
        } else {
            rows = parseCsvFile(file);
        }

        if (rows.isEmpty()) {
            throw new RuntimeException("File is empty.");
        }

        // First row = headers
        String[] headers = rows.get(0);
        Map<String, Integer> colIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String normalized = headers[i].trim().toLowerCase()
                    .replace(" ", "")
                    .replaceAll("[^a-z]", "");
            colIndex.put(normalized, i);
        }

        if (!colIndex.containsKey("name") &&
                !colIndex.containsKey("productname")) {
            throw new RuntimeException(
                    "File must have a 'Name' column. Expected: Name, Barcode, Category, CostPrice, SellingPrice, Quantity");
        }
        // Accept "productname" as alias for "name"
        if (!colIndex.containsKey("name") && colIndex.containsKey("productname")) {
            colIndex.put("name", colIndex.get("productname"));
        }

        if (!colIndex.containsKey("sellingprice") && !colIndex.containsKey("price")) {
            throw new RuntimeException(
                    "File must have a 'SellingPrice' or 'Price' column. Expected: Name, Barcode, Category, CostPrice, SellingPrice, Quantity");
        }
        if (!colIndex.containsKey("sellingprice") && colIndex.containsKey("price")) {
            colIndex.put("sellingprice", colIndex.get("price"));
        }

        List<Branch> branches = branchRepository
                .findByBusinessIdOrderByIsMainBranchDescNameAsc(businessId);

        Set<String> existingBarcodes = productRepository
                .findByBusinessIdAndActiveTrue(businessId)
                .stream()
                .map(Product::getBarcode)
                .filter(b -> b != null && !b.isBlank())
                .collect(Collectors.toSet());

        Set<String> seenBarcodes = new HashSet<>();
        int imported = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        for (int rowNum = 1; rowNum < rows.size(); rowNum++) {
            String[] cols = rows.get(rowNum);

            boolean allEmpty = true;
            for (String c : cols) { if (c != null && !c.trim().isEmpty()) { allEmpty = false; break; } }
            if (allEmpty) continue;

            try {
                String name = getCol(cols, colIndex, "name");
                if (name == null || name.isBlank()) {
                    errors.add(Map.of("row", rowNum + 1, "reason", "Name is required"));
                    continue;
                }

                String barcode  = getCol(cols, colIndex, "barcode");
                String category = getCol(cols, colIndex, "category");
                String costStr  = getCol(cols, colIndex, "costprice");
                String priceStr = getCol(cols, colIndex, "sellingprice");
                String qtyStr   = getCol(cols, colIndex, "quantity");

                // Fix barcode from scientific notation
                if (barcode != null && (barcode.contains("E") || barcode.contains("e"))) {
                    try {
                        barcode = new BigDecimal(barcode).toPlainString();
                        if (barcode.contains(".")) barcode = barcode.substring(0, barcode.indexOf('.'));
                    } catch (Exception ignored) {}
                }

                // Validate selling price
                BigDecimal sellingPrice;
                try {
                    if (priceStr == null || priceStr.isBlank()) {
                        errors.add(Map.of("row", rowNum + 1, "reason", "SellingPrice is required"));
                        continue;
                    }
                    sellingPrice = new BigDecimal(priceStr.trim());
                    if (sellingPrice.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                } catch (Exception e) {
                    errors.add(Map.of("row", rowNum + 1, "reason", "Invalid selling price: " + priceStr));
                    continue;
                }

                BigDecimal costPrice = BigDecimal.ZERO;
                if (costStr != null && !costStr.isBlank()) {
                    try { costPrice = new BigDecimal(costStr.trim()); }
                    catch (Exception e) {
                        errors.add(Map.of("row", rowNum + 1, "reason", "Invalid cost price: " + costStr));
                        continue;
                    }
                }

                double quantity = 0;
                if (qtyStr != null && !qtyStr.isBlank()) {
                    try {
                        quantity = Double.parseDouble(qtyStr.trim());
                        if (quantity < 0) quantity = 0;
                    } catch (Exception e) { quantity = 0; }
                }

                // Duplicate barcode check
                if (barcode != null && !barcode.isBlank()) {
                    if (existingBarcodes.contains(barcode) || seenBarcodes.contains(barcode)) {
                        errors.add(Map.of("row", rowNum + 1, "reason", "Duplicate barcode: " + barcode));
                        continue;
                    }
                    seenBarcodes.add(barcode);
                }

                Product product = new Product();
                product.setBusinessId(businessId);
                product.setName(name.trim());
                product.setBarcode(barcode != null ? barcode.trim() : null);
                product.setCategory(category != null ? category.trim() : null);
                product.setCostPrice(costPrice);
                product.setPrice(sellingPrice);
                product.setQuantity(quantity);
                Product saved = productRepository.save(product);

                for (Branch branch : branches) {
                    double branchQty = (branchId != null && branch.getId().equals(branchId)) ? quantity : 0;
                    branchInventoryRepository.save(new BranchInventory(branch.getId(), saved.getId(), branchQty));
                }

                imported++;
            } catch (Exception e) {
                errors.add(Map.of("row", rowNum + 1, "reason",
                        e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        result.put("skipped", errors.size());
        result.put("total", rows.size() - 1);
        result.put("errors", errors);
        return result;
    }

    // ── Parse CSV file (handles BOM) ──────────────────────────────────
    private List<String[]> parseCsvFile(MultipartFile file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        String line;
        boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if (firstLine) {
                line = line.replaceAll("^\\uFEFF", "");
                firstLine = false;
            }
            line = line.trim();
            if (line.isEmpty()) continue;
            rows.add(parseCsvLine(line));
        }
        return rows;
    }

    // ── Parse Excel file (.xlsx/.xls) via Apache POI ──────────────────
    private List<String[]> parseExcelFile(MultipartFile file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try {
            org.apache.poi.ss.usermodel.Workbook workbook =
                    org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream());
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            org.apache.poi.ss.usermodel.DataFormatter formatter =
                    new org.apache.poi.ss.usermodel.DataFormatter();

            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                int lastCell = row.getLastCellNum();
                if (lastCell < 0) continue;
                String[] cells = new String[lastCell];
                for (int i = 0; i < lastCell; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(i);
                    cells[i] = (cell == null) ? "" : formatter.formatCellValue(cell).trim();
                }
                boolean allEmpty = true;
                for (String c : cells) { if (!c.isEmpty()) { allEmpty = false; break; } }
                if (!allEmpty) rows.add(cells);
            }
            workbook.close();
        } catch (NoClassDefFoundError | Exception e) {
            throw new RuntimeException(
                    "Excel import failed. Please save your file as CSV (File → Save As → CSV) and try again.");
        }
        return rows;
    }

    // ── CSV helpers ───────────────────────────────────────────────────
    private String getCol(String[] cols, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx == null || idx >= cols.length) return null;
        String val = cols[idx].trim();
        if (val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length() - 1);
        return val.isEmpty() ? null : val;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') inQuotes = !inQuotes;
            else if (c == ',' && !inQuotes) { result.add(current.toString()); current = new StringBuilder(); }
            else current.append(c);
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    // ── Internal helpers ──────────────────────────────────────────────
    private void deleteImageFile(Product product) {
        if (product.getImagePath() != null && !product.getImagePath().isBlank()) {
            try {
                String relative = product.getImagePath().replaceFirst("^/uploads/", "");
                Path oldFile = Paths.get(uploadDir, relative);
                Files.deleteIfExists(oldFile);
            } catch (Exception ignored) {}
        }
    }

    private double resolveQty(Product product, Long branchId) {
        if (branchId == null) return product.getQuantity();
        return branchInventoryRepository
                .findByBranchIdAndProductId(branchId, product.getId())
                .map(BranchInventory::getQuantity)
                .orElse(0.0);
    }

    private ProductDTO convertToDTO(Product product, Map<Long, String> supplierNameMap,
                                    Map<Long, Double> branchQtyMap) {
        String supplierName = product.getSupplierId() != null
                ? supplierNameMap.get(product.getSupplierId()) : null;
        double qty = branchQtyMap.isEmpty()
                ? product.getQuantity()
                : branchQtyMap.getOrDefault(product.getId(), 0.0);
        return buildDTO(product, supplierName, qty);
    }

    private ProductDTO convertToDTO(Product product, String supplierName, double qty) {
        return buildDTO(product, supplierName, qty);
    }

    private ProductDTO buildDTO(Product product, String supplierName, double qty) {
        ProductDTO dto = new ProductDTO(
                product.getId(), product.getName(), product.getBarcode(),
                product.getPrice(), product.getCostPrice(), qty,
                product.getCategory(), product.getSupplierId(),
                supplierName, product.getImagePath(),
                product.getUnitType(), product.getPurity()
        );
        dto.setScrap(product.isScrap());
        dto.setTotalWeightGrams(product.getTotalWeightGrams());
        dto.setProductClass(product.getProductClass() != null ? product.getProductClass() : "JEWELLERY");
        return dto;
    }

    private Product convertToEntity(ProductDTO dto, Long businessId) {
        Product product = new Product();
        product.setBusinessId(businessId);
        product.setName(dto.getName());
        // Store null rather than empty string so unique-index permits multiple barcode-less products
        product.setBarcode((dto.getBarcode() != null && !dto.getBarcode().isBlank())
                ? dto.getBarcode().trim() : null);
        product.setPrice(dto.getPrice());
        product.setCostPrice(dto.getCostPrice());
        product.setQuantity(dto.getQuantity());
        product.setCategory(dto.getCategory());
        product.setUnitType(dto.getUnitType());
        product.setPurity(dto.getPurity());
        product.setProductClass(dto.getProductClass() != null ? dto.getProductClass() : "JEWELLERY");
        product.setScrap(dto.isScrap());
        if (dto.getSupplierId() != null) product.setSupplierId(dto.getSupplierId());
        if (dto.getTotalWeightGrams() != null) product.setTotalWeightGrams(dto.getTotalWeightGrams());
        return product;
    }
}
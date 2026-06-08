package com.business.managementsystem.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductDTO {

    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100)
    private String name;

    // Barcode is optional
    private String barcode;

    // Selling price optional (gold priced at time of sale)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    // Cost price optional
    @Digits(integer = 8, fraction = 2)
    private BigDecimal costPrice;

    @Min(value = 0)
    private double quantity;

    @NotBlank(message = "Category is required")
    @Size(min = 2, max = 50)
    private String category;

    // PCS or GRAM
    private String unitType = "PCS";

    // Purity e.g. "750", "999.9" — optional
    private String purity;

    // Optional — links this product to a supplier
    private Long supplierId;

    // Supplier name — populated when returning data, not sent on create/update
    private String supplierName;

    // URL path to product image — populated when returning data
    private String imagePath;

    // True when this is a scrap gold/silver item — purity entered per-transaction
    private boolean isScrap = false;

    // Global total weight of all pieces in grams (PCS products only; null for GRAM products)
    private Double totalWeightGrams;

    public ProductDTO() {}

    public ProductDTO(Long id, String name, String barcode,
                      BigDecimal price, BigDecimal costPrice,
                      double quantity, String category,
                      Long supplierId, String supplierName,
                      String imagePath) {
        this.id           = id;
        this.name         = name;
        this.barcode      = barcode;
        this.price        = price;
        this.costPrice    = costPrice;
        this.quantity     = quantity;
        this.category     = category;
        this.supplierId   = supplierId;
        this.supplierName = supplierName;
        this.imagePath    = imagePath;
    }

    public ProductDTO(Long id, String name, String barcode,
                      BigDecimal price, BigDecimal costPrice,
                      double quantity, String category,
                      Long supplierId, String supplierName,
                      String imagePath, String unitType, String purity) {
        this(id, name, barcode, price, costPrice, quantity, category,
             supplierId, supplierName, imagePath);
        this.unitType = unitType != null ? unitType : "PCS";
        this.purity   = purity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType != null ? unitType : "PCS"; }

    public String getPurity() { return purity; }
    public void setPurity(String purity) { this.purity = purity; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public boolean isScrap() { return isScrap; }
    public void setScrap(boolean scrap) { this.isScrap = scrap; }

    public Double getTotalWeightGrams() { return totalWeightGrams; }
    public void setTotalWeightGrams(Double totalWeightGrams) { this.totalWeightGrams = totalWeightGrams; }
}

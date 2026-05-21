package com.business.managementsystem.service;

import com.business.managementsystem.model.Product;
import com.business.managementsystem.repository.BusinessRepository;
import com.business.managementsystem.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class IndustryTemplateService {

    private final ProductRepository productRepository;
    private final BusinessRepository businessRepository;

    public IndustryTemplateService(ProductRepository productRepository, BusinessRepository businessRepository) {
        this.productRepository = productRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional
    public void applyTemplate(Long businessId, String niche) {
        if (!businessRepository.existsById(businessId)) {
            throw new RuntimeException("Business not found");
        }

        List<Product> products = new ArrayList<>();

        switch (niche.toUpperCase()) {
            case "COFFEE_SHOP":
                products.addAll(generateCoffeeShopTemplate(businessId));
                break;
            case "ELECTRONICS":
                products.addAll(generateElectronicsTemplate(businessId));
                break;
            case "GROCERY":
                products.addAll(generateGroceryTemplate(businessId));
                break;
            default:
                throw new IllegalArgumentException("Unknown niche: " + niche);
        }

        productRepository.saveAll(products);
    }

    private List<Product> generateCoffeeShopTemplate(Long businessId) {
        List<Product> list = new ArrayList<>();
        list.add(createProduct(businessId, "Espresso", "Beverages", "12.00", 500, "COF-001"));
        list.add(createProduct(businessId, "Double Espresso", "Beverages", "16.00", 300, "COF-002"));
        list.add(createProduct(businessId, "Americano", "Beverages", "14.00", 400, "COF-003"));
        list.add(createProduct(businessId, "Cappuccino", "Beverages", "18.00", 350, "COF-004"));
        list.add(createProduct(businessId, "Latte", "Beverages", "18.00", 400, "COF-005"));
        list.add(createProduct(businessId, "Flat White", "Beverages", "19.00", 300, "COF-006"));
        list.add(createProduct(businessId, "Mocha", "Beverages", "22.00", 250, "COF-007"));
        list.add(createProduct(businessId, "Iced Latte", "Beverages", "20.00", 300, "COF-008"));
        list.add(createProduct(businessId, "Iced Americano", "Beverages", "16.00", 350, "COF-009"));
        list.add(createProduct(businessId, "Matcha Latte", "Beverages", "24.00", 200, "COF-010"));
        
        list.add(createProduct(businessId, "Butter Croissant", "Food", "10.00", 150, "COF-011"));
        list.add(createProduct(businessId, "Almond Croissant", "Food", "14.00", 100, "COF-012"));
        list.add(createProduct(businessId, "Chocolate Muffin", "Food", "12.00", 120, "COF-013"));
        list.add(createProduct(businessId, "Blueberry Muffin", "Food", "12.00", 120, "COF-014"));
        list.add(createProduct(businessId, "Avocado Toast", "Food", "35.00", 50, "COF-015"));
        
        list.add(createProduct(businessId, "Coffee Beans (250g)", "Retail", "45.00", 50, "COF-016"));
        list.add(createProduct(businessId, "Reusable Cup", "Retail", "65.00", 30, "COF-017"));
        list.add(createProduct(businessId, "Almond Milk Upgrade", "Add-ons", "3.00", 1000, "COF-018"));
        list.add(createProduct(businessId, "Oat Milk Upgrade", "Add-ons", "3.00", 1000, "COF-019"));
        list.add(createProduct(businessId, "Extra Shot", "Add-ons", "4.00", 1000, "COF-020"));
        return list;
    }

    private List<Product> generateElectronicsTemplate(Long businessId) {
        List<Product> list = new ArrayList<>();
        list.add(createProduct(businessId, "USB-C Fast Charger", "Accessories", "85.00", 100, "ELE-001"));
        list.add(createProduct(businessId, "Lightning Cable 2m", "Accessories", "65.00", 150, "ELE-002"));
        list.add(createProduct(businessId, "Wireless Earbuds Pro", "Audio", "499.00", 50, "ELE-003"));
        list.add(createProduct(businessId, "Bluetooth Speaker", "Audio", "299.00", 40, "ELE-004"));
        list.add(createProduct(businessId, "Smart Watch Series X", "Wearables", "899.00", 30, "ELE-005"));
        list.add(createProduct(businessId, "Fitness Tracker", "Wearables", "199.00", 80, "ELE-006"));
        list.add(createProduct(businessId, "Smartphone Stand", "Accessories", "45.00", 120, "ELE-007"));
        list.add(createProduct(businessId, "Screen Protector - Screen Glass", "Accessories", "35.00", 200, "ELE-008"));
        list.add(createProduct(businessId, "Power Bank 10000mAh", "Power", "120.00", 60, "ELE-009"));
        list.add(createProduct(businessId, "Power Bank 20000mAh", "Power", "180.00", 40, "ELE-010"));
        
        list.add(createProduct(businessId, "HDMI Cable 4K", "Cables", "55.00", 100, "ELE-011"));
        list.add(createProduct(businessId, "Wireless Mouse", "Peripherals", "75.00", 90, "ELE-012"));
        list.add(createProduct(businessId, "Mechanical Keyboard", "Peripherals", "250.00", 30, "ELE-013"));
        list.add(createProduct(businessId, "Webcam 1080p", "Peripherals", "150.00", 45, "ELE-014"));
        list.add(createProduct(businessId, "Laptop Cooling Pad", "Accessories", "95.00", 25, "ELE-015"));
        
        list.add(createProduct(businessId, "Smart Home Hub", "Smart Home", "350.00", 20, "ELE-016"));
        list.add(createProduct(businessId, "Smart Bulb RGB", "Smart Home", "65.00", 150, "ELE-017"));
        list.add(createProduct(businessId, "Security Camera Indoor", "Smart Home", "220.00", 35, "ELE-018"));
        list.add(createProduct(businessId, "Multi-Port USB Hub", "Accessories", "85.00", 75, "ELE-019"));
        list.add(createProduct(businessId, "Noise Cancelling Headphones", "Audio", "750.00", 15, "ELE-020"));
        return list;
    }

    private List<Product> generateGroceryTemplate(Long businessId) {
        List<Product> list = new ArrayList<>();
        list.add(createProduct(businessId, "Whole Milk 1L", "Dairy", "6.50", 200, "GRO-001"));
        list.add(createProduct(businessId, "Eggs (Medium) 15-pack", "Dairy", "14.00", 150, "GRO-002"));
        list.add(createProduct(businessId, "White Bread Loaf shrink-wrapped", "Bakery", "4.50", 100, "GRO-003"));
        list.add(createProduct(businessId, "Drinking Water 1.5L", "Beverages", "1.50", 500, "GRO-004"));
        list.add(createProduct(businessId, "Orange Juice 1L", "Beverages", "8.00", 120, "GRO-005"));
        list.add(createProduct(businessId, "Tomatoes (Local) 1kg", "Produce", "6.00", 80, "GRO-006"));
        list.add(createProduct(businessId, "Onions (Red) 1kg", "Produce", "4.50", 100, "GRO-007"));
        list.add(createProduct(businessId, "Potatoes 1kg", "Produce", "5.00", 120, "GRO-008"));
        list.add(createProduct(businessId, "Bananas 1kg", "Produce", "6.50", 90, "GRO-009"));
        list.add(createProduct(businessId, "Apples (Gala) 1kg", "Produce", "8.50", 70, "GRO-010"));
        
        list.add(createProduct(businessId, "Chicken Breast 500g", "Meat", "18.00", 50, "GRO-011"));
        list.add(createProduct(businessId, "Minced Beef 500g", "Meat", "25.00", 40, "GRO-012"));
        list.add(createProduct(businessId, "Jasmine Rice 5kg", "Pantry", "35.00", 50, "GRO-013"));
        list.add(createProduct(businessId, "Pasta (Penne) 500g", "Pantry", "5.50", 100, "GRO-014"));
        list.add(createProduct(businessId, "Olive Oil 500ml", "Pantry", "22.00", 60, "GRO-015"));
        
        list.add(createProduct(businessId, "Tomato Paste", "Pantry", "3.00", 150, "GRO-016"));
        list.add(createProduct(businessId, "Salt 1kg", "Pantry", "2.50", 100, "GRO-017"));
        list.add(createProduct(businessId, "Sugar 1kg", "Pantry", "4.00", 100, "GRO-018"));
        list.add(createProduct(businessId, "Dishwashing Liquid", "Cleaning", "12.00", 80, "GRO-019"));
        list.add(createProduct(businessId, "Laundry Detergent 3kg", "Cleaning", "35.00", 40, "GRO-020"));
        return list;
    }

    private Product createProduct(Long businessId, String name, String category, String price, int qty, String barcode) {
        Product p = new Product();
        p.setBusinessId(businessId);
        p.setName(name);
        p.setCategory(category);
        BigDecimal pPrice = new BigDecimal(price);
        p.setPrice(pPrice);
        p.setCostPrice(pPrice.multiply(new BigDecimal("0.5"))); // Default margin
        p.setQuantity(qty);
        p.setBarcode(barcode);
        p.setActive(true);
        return p;
    }
}

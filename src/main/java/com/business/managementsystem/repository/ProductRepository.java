package com.business.managementsystem.repository;

import com.business.managementsystem.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Used for showing products in the Sales Terminal and Catalog
    List<Product> findByBusinessIdAndActiveTrue(Long businessId);

    long countByBusinessIdAndActiveTrue(Long businessId);

    @Query("SELECT p FROM Product p WHERE p.businessId = :businessId AND p.active = false")
    List<Product> findAllArchivedByBusinessId(Long businessId);

    Optional<Product> findByBarcodeAndBusinessId(String barcode, Long businessId);

    List<Product> findByCategoryAndBusinessIdAndActiveTrue(String category, Long businessId);

    List<Product> findByQuantityLessThanAndBusinessIdAndActiveTrue(int threshold, Long businessId);

    List<Product> findByNameContainingIgnoreCaseAndBusinessIdAndActiveTrue(String name, Long businessId);

    boolean existsByBarcodeAndBusinessId(String barcode, Long businessId);

    // Keep for legacy/admin use
    Optional<Product> findByBarcode(String barcode);
    boolean existsByBarcode(String barcode);
}
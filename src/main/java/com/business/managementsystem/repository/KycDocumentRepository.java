package com.business.managementsystem.repository;

import com.business.managementsystem.model.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findBySupplierIdOrderByUploadedAtDesc(Long supplierId);

    List<KycDocument> findBySupplierIdAndDocumentType(Long supplierId, String documentType);

    void deleteBySupplierId(Long supplierId);
}

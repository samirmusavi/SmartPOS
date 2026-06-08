package com.business.managementsystem.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_document")
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false, length = 50)
    private String documentType;   // e.g. EMIRATES_ID, PASSPORT, TRADE_LICENSE, OTHER

    @Column(nullable = false, length = 255)
    private String fileName;       // original filename shown to user

    @Column(nullable = false, length = 500)
    private String filePath;       // absolute path on disk

    @Column
    private Long fileSize;         // bytes

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    @Column(length = 255)
    private String uploadedBy;

    public KycDocument() {}

    public KycDocument(Long supplierId, String documentType,
                       String fileName, String filePath,
                       Long fileSize, String uploadedBy) {
        this.supplierId    = supplierId;
        this.documentType  = documentType;
        this.fileName      = fileName;
        this.filePath      = filePath;
        this.fileSize      = fileSize;
        this.uploadedBy    = uploadedBy;
    }

    public Long getId()           { return id; }
    public Long getSupplierId()   { return supplierId; }

    public String getDocumentType()  { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getFileName()  { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath()  { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize()    { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}

package com.infosys.svpms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotation_documents")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class QuotationDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    private Long fileSize;

    @Column(length = 100)
    private String fileType;

    @Column(length = 64)
    private String checksum; // SHA-256 hash for file integrity

    @CreationTimestamp
    private LocalDateTime uploadedAt;
}

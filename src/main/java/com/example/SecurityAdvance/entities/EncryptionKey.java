package com.example.SecurityAdvance.entities;

import com.example.SecurityAdvance.enums.EncryptionKeyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "encryption_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncryptionKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mỗi key thuộc về 1 user
     * 1 user có thể có nhiều key (rotate)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "key_name", nullable = false, length = 100)
    private String keyName; // ví dụ: USER_PII_KEY

    @Column(name = "encrypted_dek", nullable = false, columnDefinition = "TEXT")
    private String encryptedDek; // DEK wrapped

    @Column(name = "encrypted_kek", nullable = false, columnDefinition = "TEXT")
    private String encryptedKek; // DEK wrapped

    @Column(nullable = false, length = 64)
    private String keyIv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EncryptionKeyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.status = this.status == null ? EncryptionKeyStatus.ACTIVE : this.status;
    }
}

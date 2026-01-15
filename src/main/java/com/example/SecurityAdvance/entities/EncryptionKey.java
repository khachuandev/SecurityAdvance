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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "key_name", nullable = false, length = 100)
    private String keyName; // ví dụ: USER_PII_KEY

    @Column(name = "encrypted_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedKey; // DEK wrapped

    @Column(nullable = false, length = 64)
    private String iv;

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

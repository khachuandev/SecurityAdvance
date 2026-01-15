package com.example.SecurityAdvance.entities;

import com.example.SecurityAdvance.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String phoneNumber;

    @Column(nullable = false, unique = true, length = 64)
    private String emailHash;

    @Column(nullable = false, unique = true, length = 64)
    private String phoneHash;

    @Column(name = "data_iv", nullable = false, length = 64)
    private String dataIv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encryption_key_id")
    private EncryptionKey activeEncryptionKey;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private UserStatus status;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_login")
    private Instant lastLogin;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.status = this.status == null ? UserStatus.ACTIVE : this.status;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

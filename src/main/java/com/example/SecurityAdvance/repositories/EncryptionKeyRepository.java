package com.example.SecurityAdvance.repositories;

import com.example.SecurityAdvance.entities.EncryptionKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncryptionKeyRepository extends JpaRepository<EncryptionKey, Long> {
}

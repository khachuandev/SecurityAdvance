package com.example.SecurityAdvance.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

@Slf4j
@Component
public class RSAUtils {
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    @Value("${rsa.keystore.path}")
    private String keystorePath;

    @Value("${rsa.keystore.password}")
    private String keystorePassword;

    @Value("${rsa.key.alias}")
    private String keyAlias;

    /* ========== LOAD KEYSTORE ========== */
    private KeyStore loadKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

            InputStream is = keystorePath.startsWith("classpath:")
                    ? new ClassPathResource(keystorePath.replace("classpath:", "")).getInputStream()
                    : new FileInputStream(keystorePath);

            keyStore.load(is, keystorePassword.toCharArray());
            return keyStore;
        } catch (Exception e) {
            log.error("Failed to load PKCS12 keystore from: {}", keystorePath, e);
            throw new RuntimeException("Load PKCS12 keystore failed", e);
        }
    }

    public PrivateKey getPrivateKey() {
        try {
            return (PrivateKey) loadKeyStore().getKey(keyAlias, keystorePassword.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException("Load private key failed", e);
        }
    }

    public PublicKey getPublicKey() {
        try {
            return loadKeyStore().getCertificate(keyAlias).getPublicKey();
        } catch (Exception e) {
            throw new RuntimeException("Load public key failed", e);
        }
    }

    /* ========== RSA FOR AES KEY ========== */
    public String encryptAESKey(String aesKeyBase64) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getPublicKey());
            byte[] encrypted = cipher.doFinal(
                    aesKeyBase64.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encrypt AES key with RSA failed", e);
        }
    }

    public String decryptAESKey(String encryptedAESKeyBase64) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
            byte[] decrypted = cipher.doFinal(
                    Base64.getDecoder().decode(encryptedAESKeyBase64)
            );
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decrypt AES key with RSA failed", e);
        }
    }

    /* ========== SIGN ========== */
    public String sign(String data) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(getPrivateKey());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            log.error("RSA sign failed", e);
            throw new RuntimeException("RSA sign failed", e);
        }
    }

    public boolean verify(String data, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(getPublicKey());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }
}

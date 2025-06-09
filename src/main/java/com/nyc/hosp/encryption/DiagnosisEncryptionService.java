package com.nyc.hosp.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class DiagnosisEncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int AES_KEY_BIT = 256;
    
    @Value("${encryption.key:DefaultSecretKeyMustBe32BytesLong!}")
    private String masterKey;
    
    private SecretKey getKey() {
        // In production, this should be properly managed (e.g., AWS KMS, HashiCorp Vault)
        byte[] keyBytes = masterKey.getBytes(StandardCharsets.UTF_8);
        
        // Ensure key is 32 bytes for AES-256
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        } else if (keyBytes.length > 32) {
            byte[] truncatedKey = new byte[32];
            System.arraycopy(keyBytes, 0, truncatedKey, 0, 32);
            keyBytes = truncatedKey;
        }
        
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    public String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        // Generate IV
        byte[] iv = new byte[IV_LENGTH_BYTE];
        new SecureRandom().nextBytes(iv);
        
        // Get cipher instance
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        
        // Create GCMParameterSpec
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        
        // Initialize cipher for encryption
        cipher.init(Cipher.ENCRYPT_MODE, getKey(), gcmParameterSpec);
        
        // Perform encryption
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        
        // Combine IV and ciphertext
        byte[] cipherTextWithIv = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, cipherTextWithIv, 0, iv.length);
        System.arraycopy(cipherText, 0, cipherTextWithIv, iv.length, cipherText.length);
        
        // Return base64 encoded string
        return Base64.getEncoder().encodeToString(cipherTextWithIv);
    }
    
    public String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        // Decode from base64
        byte[] cipherTextWithIv = Base64.getDecoder().decode(encryptedText);
        
        // Extract IV
        byte[] iv = new byte[IV_LENGTH_BYTE];
        System.arraycopy(cipherTextWithIv, 0, iv, 0, iv.length);
        
        // Extract cipher text
        byte[] cipherText = new byte[cipherTextWithIv.length - iv.length];
        System.arraycopy(cipherTextWithIv, iv.length, cipherText, 0, cipherText.length);
        
        // Get cipher instance
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        
        // Create GCMParameterSpec
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        
        // Initialize cipher for decryption
        cipher.init(Cipher.DECRYPT_MODE, getKey(), gcmParameterSpec);
        
        // Perform decryption
        byte[] plainText = cipher.doFinal(cipherText);
        
        return new String(plainText, StandardCharsets.UTF_8);
    }
    
    // Method to demonstrate encryption for testing
    public String demonstrateEncryption(String diagnosis) {
        try {
            String encrypted = encrypt(diagnosis);
            System.out.println("[ENCRYPTION] Original: " + diagnosis);
            System.out.println("[ENCRYPTION] Encrypted: " + encrypted);
            
            String decrypted = decrypt(encrypted);
            System.out.println("[ENCRYPTION] Decrypted: " + decrypted);
            
            return encrypted;
        } catch (Exception e) {
            System.err.println("[ENCRYPTION] Error: " + e.getMessage());
            return "Encryption error: " + e.getMessage();
        }
    }
}

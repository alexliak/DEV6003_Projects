package com.nyc.hosp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;
    
    @Value("${encryption.key:YourEncryptionKeyMustBe32BytesLong!}")
    private String masterKey;
    
    public String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Generate random IV
            byte[] iv = generateIV();
            
            // Get secret key
            SecretKey key = getSecretKey();
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            
            // Encrypt the data
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and ciphertext
            byte[] encrypted = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(cipherText, 0, encrypted, iv.length, cipherText.length);
            
            // Return base64 encoded
            return Base64.getEncoder().encodeToString(encrypted);
            
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Decode from base64
            byte[] encrypted = Base64.getDecoder().decode(encryptedText);
            
            // Extract IV and ciphertext
            byte[] iv = Arrays.copyOfRange(encrypted, 0, IV_SIZE);
            byte[] cipherText = Arrays.copyOfRange(encrypted, IV_SIZE, encrypted.length);
            
            // Get secret key
            SecretKey key = getSecretKey();
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            
            // Decrypt the data
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            // Log the error but don't expose details
            System.err.println("Decryption failed for text: " + encryptedText);
            return "[Decryption Failed]";
        }
    }
    
    private byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    
    private SecretKey getSecretKey() {
        // Ensure key is exactly 32 bytes
        byte[] keyBytes = Arrays.copyOf(masterKey.getBytes(StandardCharsets.UTF_8), 32);
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    // Method to check if a string is already encrypted
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        try {
            // Try to decode as base64
            byte[] decoded = Base64.getDecoder().decode(text);
            // Check if length is at least IV_SIZE + 1 byte of data
            return decoded.length > IV_SIZE;
        } catch (Exception e) {
            return false;
        }
    }
}

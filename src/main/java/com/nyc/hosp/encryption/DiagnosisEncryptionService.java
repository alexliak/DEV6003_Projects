package com.nyc.hosp.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class DiagnosisEncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;
    
    @Value("${encryption.key}")
    private String masterKey;
    
    public String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        
        byte[] iv = generateIV();
        SecretKey key = generateKey();
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = new byte[iv.length + cipherText.length];
        
        System.arraycopy(iv, 0, encrypted, 0, iv.length);
        System.arraycopy(cipherText, 0, encrypted, iv.length, cipherText.length);
        
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    public String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }
        
        byte[] encrypted = Base64.getDecoder().decode(encryptedText);
        byte[] iv = Arrays.copyOfRange(encrypted, 0, IV_SIZE);
        byte[] cipherText = Arrays.copyOfRange(encrypted, IV_SIZE, encrypted.length);
        
        SecretKey key = generateKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        
        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }
    
    private byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    
    private SecretKey generateKey() {
        // Ensure key is exactly 32 bytes for AES-256
        byte[] keyBytes = masterKey.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
        return new SecretKeySpec(key, "AES");
    }
}

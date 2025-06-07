package com.nyc.hosp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        
        // Test passwords
        String[] passwords = {"1234", "Password123!"};
        
        for (String password : passwords) {
            String encoded = encoder.encode(password);
            System.out.println("Password: " + password);
            System.out.println("Encoded: " + encoded);
            System.out.println("---");
        }
        
        // Test if the hash in DB matches
        String hashInDB = "$2a$12$YDW7rprweETfXBr8PAUcFOSVpv0pADLtnQv/I9BbauNnmVZQOmPPa";
        System.out.println("Hash in DB matches '1234': " + encoder.matches("1234", hashInDB));
    }
}

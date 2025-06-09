package com.nyc.hosp.model;

import com.nyc.hosp.domain.Hospuser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @ManyToOne
    @JoinColumn(nullable = false, name = "user_id")
    private Hospuser user;
    
    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(nullable = false)
    private boolean used = false;
    
    // Constructors
    public PasswordResetToken() {}
    
    public PasswordResetToken(String token, Hospuser user) {
        this.token = token;
        this.user = user;
        // Token expires in 24 hours
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }
    
    // Check if token is expired
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Hospuser getUser() {
        return user;
    }
    
    public void setUser(Hospuser user) {
        this.user = user;
    }
    
    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public boolean isUsed() {
        return used;
    }
    
    public void setUsed(boolean used) {
        this.used = used;
    }
}

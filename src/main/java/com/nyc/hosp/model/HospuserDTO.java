package com.nyc.hosp.model;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;


public class HospuserDTO {

    private Long userId;

    @Size(max = 100)
    //@Pattern()
    private String username;

    @Size(max = 100)
    private String userpassword;

    @Size(max = 100)
    private String userpassword2;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime lastlogondatetime;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime lastchangepassword;

    @Size(max = 100)
    private String email;

    private boolean locked;

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    private Integer role;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getUserpassword() {
        return userpassword;
    }

    public void setUserpassword(final String userpassword) {
        this.userpassword = userpassword;
    }

    public OffsetDateTime getLastlogondatetime() {
        return lastlogondatetime;
    }

    public void setLastlogondatetime(final OffsetDateTime lastlogondatetime) {
        this.lastlogondatetime = lastlogondatetime;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(final Integer role) {
        this.role = role;
    }

    public String getUserpassword2() {
        return userpassword2;
    }

    public void setUserpassword2(String userpassword2) {
        this.userpassword2 = userpassword2;
    }

    public OffsetDateTime getLastchangepassword() {
        return lastchangepassword;
    }

    public void setLastchangepassword(OffsetDateTime lastchangepasswordl) {
        this.lastchangepassword = lastchangepasswordl;
    }
}

package com.nyc.hosp.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


@Entity
public class Hospuser {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column
    private OffsetDateTime lastlogondatetime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastPasswordChange;
    
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(length = 100, unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean accountLocked = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(final Set<Role> roles) {
        this.roles = roles;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    public Date getLastPasswordChange() {
        return lastPasswordChange;
    }

    public void setLastPasswordChange(Date lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }
    
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }
}

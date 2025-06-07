package com.nyc.hosp.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class PasswordChangeRequest {
    
    @NotBlank(message = "Current password is required")
    private String oldPassword;
    
    @NotBlank(message = "New password is required")
    private String newPassword;
    
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
    
    public PasswordChangeRequest() {
    }
    
    public String getOldPassword() {
        return oldPassword;
    }
    
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
    
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}

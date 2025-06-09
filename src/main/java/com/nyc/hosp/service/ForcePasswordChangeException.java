package com.nyc.hosp.service;

public class ForcePasswordChangeException extends RuntimeException {
    public ForcePasswordChangeException(String message) {
        super(message);
    }
}

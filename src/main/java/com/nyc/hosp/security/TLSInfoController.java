package com.nyc.hosp.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tls")
public class TLSInfoController {
    
    @GetMapping("/info")
    public Map<String, Object> getTLSInfo(HttpServletRequest request) {
        Map<String, Object> tlsInfo = new HashMap<>();
        
        // Check if connection is secure
        tlsInfo.put("isSecure", request.isSecure());
        tlsInfo.put("protocol", request.getProtocol());
        tlsInfo.put("scheme", request.getScheme());
        
        // Get TLS/SSL information
        String protocol = (String) request.getAttribute("javax.servlet.request.ssl_session_id");
        tlsInfo.put("sslSessionId", protocol != null ? "Present (Session encrypted)" : "Not found");
        
        // Get cipher suite
        String cipherSuite = (String) request.getAttribute("javax.servlet.request.cipher_suite");
        tlsInfo.put("cipherSuite", cipherSuite != null ? cipherSuite : "Not available");
        
        // Get key size
        Integer keySize = (Integer) request.getAttribute("javax.servlet.request.key_size");
        tlsInfo.put("keySize", keySize != null ? keySize + " bits" : "Not available");
        
        // Get client certificates if any
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        tlsInfo.put("clientCertificate", certs != null ? "Present" : "Not using client certificates");
        
        // Session information
        tlsInfo.put("sessionId", request.getSession().getId());
        tlsInfo.put("sessionCreated", request.getSession().getCreationTime());
        tlsInfo.put("sessionTimeout", request.getSession().getMaxInactiveInterval() + " seconds");
        
        // Security headers check
        Map<String, String> headers = new HashMap<>();
        headers.put("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        headers.put("X-Content-Type-Options", "nosniff");
        headers.put("X-Frame-Options", "DENY");
        headers.put("X-XSS-Protection", "1; mode=block");
        headers.put("Content-Security-Policy", "default-src 'self'");
        tlsInfo.put("securityHeaders", headers);
        
        return tlsInfo;
    }
}

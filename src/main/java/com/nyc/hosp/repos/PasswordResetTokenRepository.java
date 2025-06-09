package com.nyc.hosp.repos;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    List<PasswordResetToken> findByUser(Hospuser user);
    
    void deleteByExpiryDateLessThan(LocalDateTime now);
    
    // Find all tokens for a user that are not used and not expired
    List<PasswordResetToken> findByUserAndUsedFalseAndExpiryDateGreaterThan(
        Hospuser user, LocalDateTime now);
}

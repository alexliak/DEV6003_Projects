package com.nyc.hosp.repos;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface HospuserRepository extends JpaRepository<Hospuser, Long> {

    Optional<Hospuser> findByUsername(String username);
    
    Optional<Hospuser> findByEmail(String email);
    
    Optional<Hospuser> findByUsernameOrEmail(String username, String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    @Query("SELECT u FROM Hospuser u JOIN u.roles r WHERE r.name = :roleName")
    List<Hospuser> findByRoleName(@Param("roleName") Role.RoleName roleName);
    
    @Query("SELECT u FROM Hospuser u WHERE u.accountLocked = true")
    List<Hospuser> findAllLockedAccounts();
}

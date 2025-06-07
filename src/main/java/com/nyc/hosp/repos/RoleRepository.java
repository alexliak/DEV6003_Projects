package com.nyc.hosp.repos;

import com.nyc.hosp.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Role findByName(Role.RoleName name);
    
    boolean existsByName(Role.RoleName name);
}

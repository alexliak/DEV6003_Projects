package com.nyc.hosp.security;

import com.nyc.hosp.domain.Hospuser;
import com.nyc.hosp.repos.HospuserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private HospuserRepository hospuserRepository;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Let people login with either username or email
        Hospuser user = hospuserRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> 
                        new UsernameNotFoundException("User not found with username or email : " + usernameOrEmail)
                );
        
        return CustomUserPrincipal.create(user);
    }
    
    // This method is used by JWTAuthenticationFilter
    @Transactional
    public UserDetails loadUserById(Long id) {
        Hospuser user = hospuserRepository.findById(id)
                .orElseThrow(() -> 
                    new UsernameNotFoundException("User not found with id : " + id)
                );
        
        return CustomUserPrincipal.create(user);
    }
}

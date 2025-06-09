package com.nyc.hosp.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;
    
    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;
    
    @Autowired
    private CustomAuthenticationFailureHandler failureHandler;
    
    // Remove the DaoAuthenticationProvider bean - we use CustomAuthenticationProvider
    /*
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    */
    
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(customAuthenticationProvider);
        return authenticationManagerBuilder.build();
    }
    
    // API Security (JWT) - Order 1
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/test/**").authenticated() // Allow test endpoints for authenticated users
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/visits/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers("/api/patients/**").hasAnyRole("ADMIN", "DOCTOR", "SECRETARIAT")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    // Web Security (Session-based with HTTPS) - Order 2
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            // Force HTTPS
            .requiresChannel(channel -> channel
                .anyRequest().requiresSecure()
            )
            
            // Headers for security
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                    .preload(true)
                )
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com")
                )
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                .contentTypeOptions(Customizer.withDefaults())
            )
            
            // Traditional Form Login
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
            )
            
            // Logout
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            
            // Authorization
            .authorizeHttpRequests(authz -> authz
                // Public resources
                .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/auth/**", "/error").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                .requestMatchers("/force-password-change", "/password-change-success").authenticated()
                
                // Role-based access
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/roles/**").hasRole("ADMIN")
                .requestMatchers("/hospusers/**").hasAnyRole("ADMIN", "SECRETARIAT")
                .requestMatchers("/patientvisits/**").hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers("/security-status").authenticated()
                .requestMatchers("/security-demo").authenticated()
                
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}

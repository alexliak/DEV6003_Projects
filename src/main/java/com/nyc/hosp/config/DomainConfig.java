package com.nyc.hosp.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Configuration
@EntityScan({"com.nyc.hosp.domain", "com.nyc.hosp.model"})
@EnableJpaRepositories("com.nyc.hosp.repos")
@EnableTransactionManagement
public class DomainConfig {
}

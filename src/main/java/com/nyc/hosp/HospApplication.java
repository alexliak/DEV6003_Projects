package com.nyc.hosp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
public class HospApplication {

    public static void main(final String[] args) {
        SpringApplication.run(HospApplication.class, args);
    }

}

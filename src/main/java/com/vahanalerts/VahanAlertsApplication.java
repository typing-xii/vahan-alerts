package com.vahanalerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VahanAlertsApplication {

    public static void main(String[] args) {
        SpringApplication.run(VahanAlertsApplication.class, args);
    }
}

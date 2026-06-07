package com.smartinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Application entry point for the Smart Inventory Prediction System.
 *
 * <p>{@code @EnableJpaAuditing} activates automatic population of
 * {@code @CreatedDate} / {@code @LastModifiedDate} fields on auditable entities.
 */
@SpringBootApplication
@EnableJpaAuditing
public class SmartInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartInventoryApplication.class, args);
    }
}

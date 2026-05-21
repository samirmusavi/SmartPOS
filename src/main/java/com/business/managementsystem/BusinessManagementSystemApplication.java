package com.business.managementsystem;

import com.business.managementsystem.service.AdminService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BusinessManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                BusinessManagementSystemApplication.class, args);
    }

    @Bean
    CommandLineRunner init(AdminService adminService) {
        return args -> adminService.initializeDefaultAdmin();
    }
}
package com.business.managementsystem.config;

import com.business.managementsystem.service.BranchService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs once on every application startup.
 *
 * 1. Ensures every existing business has a Main Branch created.
 *    Safe to run multiple times — skips businesses that already have a branch.
 *
 * 2. Ensures every existing supply has a branch_supply_inventory row
 *    for every branch of its business. New rows default to 0 quantity.
 *    Safe to run multiple times — skips rows that already exist.
 */
@Component
public class BranchMigrationRunner implements ApplicationRunner {

    private final BranchService branchService;

    public BranchMigrationRunner(BranchService branchService) {
        this.branchService = branchService;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("[SmartPOS] Running branch migration check...");
        branchService.migrateExistingBusinesses();
        System.out.println("[SmartPOS] Branch migration complete.");

        System.out.println("[SmartPOS] Running supply inventory migration check...");
        branchService.migrateSupplyInventory();
        System.out.println("[SmartPOS] Supply inventory migration complete.");
    }
}
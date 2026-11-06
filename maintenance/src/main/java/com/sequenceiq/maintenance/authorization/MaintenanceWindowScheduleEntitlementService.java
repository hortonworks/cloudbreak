package com.sequenceiq.maintenance.authorization;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Service
public class MaintenanceWindowScheduleEntitlementService {

    private static final String MESSAGE =
            "Maintenance window schedule APIs require the CDP_MAINTENANCE_WINDOW entitlement for account: %s";

    private final EntitlementService entitlementService;

    @Inject
    public MaintenanceWindowScheduleEntitlementService(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    public void verifyEntitled(String accountId) {
        if (!entitlementService.isMaintenanceWindowEnabled(accountId)) {
            throw new ForbiddenException(String.format(MESSAGE, accountId));
        }
    }
}

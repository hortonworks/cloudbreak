package com.sequenceiq.cloudbreak.service.database;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.common.model.AzureDatabaseType;

@Service
public class EnvironmentDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDatabaseService.class);

    @Inject
    private EntitlementService entitlementService;

    @Nonnull
    public AzureDatabaseType validateOrModifyDatabaseTypeIfNeeded(AzureDatabaseType azureDatabaseType) {
        AzureDatabaseType modifiedDbType = azureDatabaseType;
        if (azureDatabaseType == null) {
            modifiedDbType = AzureDatabaseType.FLEXIBLE_SERVER;
            LOGGER.info("Azure Database Type set to {} because it is not given in the database request", modifiedDbType);
        }
        return modifiedDbType;
    }

}
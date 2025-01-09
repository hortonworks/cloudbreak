package com.sequenceiq.cloudbreak.service.database;

import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.common.model.AzureDatabaseType;

@Service
public class EnvironmentDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDatabaseService.class);

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
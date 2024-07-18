package com.sequenceiq.cloudbreak.service.database;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EnvironmentDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDatabaseService.class);

    @Inject
    private EntitlementService entitlementService;

    @Nonnull
    public AzureDatabaseType validateOrModifyDatabaseTypeIfNeeded(DetailedEnvironmentResponse environment, AzureDatabaseType azureDatabaseType) {
        AzureDatabaseType modifiedDbType = azureDatabaseType;
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        boolean azureDatabaseFlexibleServerEnabled = entitlementService.isAzureDatabaseFlexibleServerEnabled(accountId);
        if (azureDatabaseType == null) {
            modifiedDbType = azureDatabaseFlexibleServerEnabled ? AzureDatabaseType.FLEXIBLE_SERVER : AzureDatabaseType.SINGLE_SERVER;
            LOGGER.info("Azure Database Type set to {} because it is not given in the database request", modifiedDbType);
        }
        if (modifiedDbType == AzureDatabaseType.FLEXIBLE_SERVER && !azureDatabaseFlexibleServerEnabled) {
            LOGGER.info("Azure Flexible Database Server creation is not entitled for {} account", accountId);
            throw new BadRequestException("You are not entitled to use Flexible Database Server on Azure for your cluster." +
                    " Please contact Cloudera to enable " + Entitlement.CDP_AZURE_DATABASE_FLEXIBLE_SERVER + " for your account");
        }
        return modifiedDbType;
    }

}
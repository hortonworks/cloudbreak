package com.sequenceiq.cloudbreak.service.database;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

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
        if (isAzurePrivateSingleServerSetup(environment)) {
            if (azureDatabaseType == AzureDatabaseType.FLEXIBLE_SERVER) {
                throw new BadRequestException("Your environment was created with Azure Private Single Server database setup." +
                        " If you would like to start your cluster with Private Flexible Server database, you have to change your environment network setup. " +
                        "Please refer to the documentation at " + DocumentationLinkProvider.azureFlexibleServerForExistingEnvironmentLink());
            } else {
                LOGGER.info("Fall back to Azure Private Single Server because the environment set up with private single server settings");
                modifiedDbType = AzureDatabaseType.SINGLE_SERVER;
            }
        } else if (azureDatabaseType == null) {
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

    private boolean isAzurePrivateSingleServerSetup(DetailedEnvironmentResponse environment) {
        boolean result = false;
        if (environment != null && environment.getNetwork() != null && environment.getNetwork().getAzure() != null) {
            EnvironmentNetworkResponse environmentNetwork = environment.getNetwork();
            EnvironmentNetworkAzureParams azureParams = environmentNetwork.getAzure();
            if ((environmentNetwork.getServiceEndpointCreation() == ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT
                    || StringUtils.isNotEmpty(azureParams.getDatabasePrivateDnsZoneId()))
                    && CollectionUtils.isEmpty(azureParams.getFlexibleServerSubnetIds())) {
                result = true;
            }
        }
        return result;
    }
}

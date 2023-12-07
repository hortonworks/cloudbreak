package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.FLEXIBLE_SERVER_DELEGATED_SUBNET_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class DatabaseRequestToDatabaseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseRequestToDatabaseConverter.class);

    @Inject
    private EntitlementService entitlementService;

    public Database convert(DetailedEnvironmentResponse environment, CloudPlatform cloudPlatform, DatabaseRequest source) {
        Database database = new Database();
        if (source != null) {
            database.setExternalDatabaseAvailabilityType(Optional.ofNullable(source.getAvailabilityType()).orElse(DatabaseAvailabilityType.NONE));
            database.setExternalDatabaseEngineVersion(source.getDatabaseEngineVersion());
            database.setAttributes(configureAzureDatabaseIfNeeded(environment, cloudPlatform, source, source.getAvailabilityType()).orElse(null));
            database.setDatalakeDatabaseAvailabilityType(source.getDatalakeDatabaseAvailabilityType());
        }
        return database;
    }

    private Optional<Json> configureAzureDatabaseIfNeeded(DetailedEnvironmentResponse environment, CloudPlatform cloudPlatform, DatabaseRequest databaseRequest,
            DatabaseAvailabilityType availabilityType) {
        if (cloudPlatform == CloudPlatform.AZURE && availabilityType != null && !availabilityType.isEmbedded()) {
            AzureDatabaseType azureDatabaseType = Optional.ofNullable(databaseRequest)
                    .map(DatabaseRequest::getDatabaseAzureRequest)
                    .map(DatabaseAzureRequest::getAzureDatabaseType)
                    .orElse(null);
            azureDatabaseType = validateOrModifyDatabaseTypeIfNeeded(environment, azureDatabaseType);
            Optional<String> delegatedSubnetID = Optional.ofNullable(databaseRequest)
                    .map(DatabaseRequest::getDatabaseAzureRequest)
                    .map(DatabaseAzureRequest::getFlexibleServerDelegatedSubnetId);
            Map<String, Object> params = new HashMap<>();
            params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name());
            delegatedSubnetID.ifPresent(subnet -> params.put(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID, subnet));
            return Optional.of(new Json(params));
        } else if (cloudPlatform == CloudPlatform.AZURE && databaseRequest.getDatabaseAzureRequest() != null
                && databaseRequest.getDatabaseAzureRequest().getAzureDatabaseType() != null) {
            Map<String, Object> params = createParamsWithDatabaseType(databaseRequest);
            addDelegatedSubnetId(databaseRequest, params);
            return Optional.of(new Json(params));
        } else {
            return Optional.empty();
        }
    }

    private AzureDatabaseType validateOrModifyDatabaseTypeIfNeeded(DetailedEnvironmentResponse environment, AzureDatabaseType azureDatabaseType) {
        AzureDatabaseType modifiedDbType = azureDatabaseType;
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        boolean azureDatabaseFlexibleServerEnabled = entitlementService.isAzureDatabaseFlexibleServerEnabled(accountId);
        if (isAzurePrivateSingleServerSetup(environment)) {
            if (azureDatabaseType == AzureDatabaseType.FLEXIBLE_SERVER) {
                throw new BadRequestException("Your environment was created with Azure Private Single Server database setup." +
                        " If you would like to start your DataHub with Private Flexible Server database, you have to change your environment network setup.");
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

    private static Map<String, Object> createParamsWithDatabaseType(DatabaseRequest databaseRequest) {
        Map<String, Object> params = new HashMap<>();
        params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, databaseRequest.getDatabaseAzureRequest().getAzureDatabaseType().name());
        return params;
    }

    private static void addDelegatedSubnetId(DatabaseRequest databaseRequest, Map<String, Object> params) {
        String flexibleServerDelegatedSubnetId = databaseRequest.getDatabaseAzureRequest().getFlexibleServerDelegatedSubnetId();
        if (flexibleServerDelegatedSubnetId != null) {
            params.put(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID, flexibleServerDelegatedSubnetId);
        }
    }
}

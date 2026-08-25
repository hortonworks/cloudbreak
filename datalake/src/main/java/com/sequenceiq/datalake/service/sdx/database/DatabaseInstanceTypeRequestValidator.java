package com.sequenceiq.datalake.service.sdx.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeCapabilityValidator;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeValidationInput;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeValidationInput.InstanceTypeSpecs;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@Component
public class DatabaseInstanceTypeRequestValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInstanceTypeRequestValidator.class);

    @Inject
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Inject
    private DatabaseInstanceTypeCapabilityValidator capabilityValidator;

    @Inject
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    public void validateIfPresent(SdxDatabaseRequest databaseRequest, DatabaseRequest internalDatabaseRequest,
            DetailedEnvironmentResponse env, Architecture desiredArchitecture, String initiatorUserCrn) {
        String requestedInstanceType = resolveInstanceType(databaseRequest, internalDatabaseRequest);
        if (StringUtils.isBlank(requestedInstanceType)) {
            return;
        }
        String regionName = env.getLocation().getName();
        DatabaseCapabilityType capabilityType = determineCapabilityType(env.getCloudPlatform(), databaseRequest, internalDatabaseRequest);
        String archName = desiredArchitecture != null ? desiredArchitecture.getName() : null;
        try {
            PlatformDatabaseCapabilitiesResponse capabilities = ThreadBasedUserCrnProvider.doAs(initiatorUserCrn,
                    () -> environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                            env.getCrn(), regionName, env.getCloudPlatform(), null, capabilityType, archName));
            DatabaseInstanceTypeValidationInput input = buildInput(requestedInstanceType, regionName, desiredArchitecture, capabilities);
            capabilityValidator.validate(input);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Could not validate database instance type '{}' for region '{}'. Proceeding without validation.",
                    requestedInstanceType, regionName, e);
        }
    }

    private String resolveInstanceType(SdxDatabaseRequest databaseRequest, DatabaseRequest internalDatabaseRequest) {
        return Optional.ofNullable(databaseRequest)
                .map(SdxDatabaseRequest::getDatabaseInstanceType)
                .filter(StringUtils::isNotBlank)
                .orElse(Optional.ofNullable(internalDatabaseRequest)
                        .map(DatabaseRequest::getDatabaseInstanceType)
                        .orElse(null));
    }

    private DatabaseCapabilityType determineCapabilityType(String cloudPlatform, SdxDatabaseRequest databaseRequest,
            DatabaseRequest internalDatabaseRequest) {
        if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            AzureDatabaseType azureType = azureDatabaseAttributesService.determineAzureDatabaseType(internalDatabaseRequest, databaseRequest);
            if (AzureDatabaseType.FLEXIBLE_SERVER.equals(azureType)) {
                return DatabaseCapabilityType.AZURE_FLEXIBLE;
            } else if (AzureDatabaseType.SINGLE_SERVER.equals(azureType)) {
                return DatabaseCapabilityType.AZURE_SINGLE_SERVER;
            }
        }
        return DatabaseCapabilityType.DEFAULT;
    }

    private DatabaseInstanceTypeValidationInput buildInput(String requestedType, String regionName,
            Architecture desiredArchitecture, PlatformDatabaseCapabilitiesResponse capabilities) {
        String defaultType = capabilities.getRegionDefaultInstances().get(regionName);
        Set<DatabaseVmTypeResponse> regionTypes = capabilities.getDatabaseVmTypes().getOrDefault(regionName, Set.of());
        Map<String, InstanceTypeSpecs> availableTypes = new HashMap<>();
        for (DatabaseVmTypeResponse resp : regionTypes) {
            Map<String, Object> props = Optional.ofNullable(resp.getDatabaseVmTypeMetaJson())
                    .map(DatabaseVmTypeMetaJson::getProperties)
                    .orElse(Map.of());
            availableTypes.put(resp.getValue(), InstanceTypeSpecs.fromProperties(props));
        }
        return new DatabaseInstanceTypeValidationInput(regionName, requestedType, defaultType, desiredArchitecture, availableTypes);
    }
}

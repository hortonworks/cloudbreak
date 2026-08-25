package com.sequenceiq.cloudbreak.service.externaldatabase;

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

@Component
public class DatabaseInstanceTypeRequestValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInstanceTypeRequestValidator.class);

    @Inject
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Inject
    private DatabaseInstanceTypeCapabilityValidator capabilityValidator;

    public void validateIfPresent(String requestedInstanceType, DatabaseRequest databaseRequest,
            DetailedEnvironmentResponse env, Architecture desiredArchitecture) {
        if (StringUtils.isBlank(requestedInstanceType)) {
            return;
        }
        String regionName = env.getLocation().getName();
        DatabaseCapabilityType capabilityType = determineCapabilityType(env.getCloudPlatform(), databaseRequest);
        String archName = desiredArchitecture != null ? desiredArchitecture.getName() : null;
        try {
            PlatformDatabaseCapabilitiesResponse capabilities = environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                    env.getCrn(), regionName, env.getCloudPlatform(), null, capabilityType, archName);
            DatabaseInstanceTypeValidationInput input = buildInput(requestedInstanceType, regionName, desiredArchitecture, capabilities);
            capabilityValidator.validate(input);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Could not validate database instance type '{}' for region '{}'. Proceeding without validation.",
                    requestedInstanceType, regionName, e);
        }
    }

    private DatabaseCapabilityType determineCapabilityType(String cloudPlatform, DatabaseRequest databaseRequest) {
        if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform) && databaseRequest != null && databaseRequest.getDatabaseAzureRequest() != null) {
            AzureDatabaseType azureType = databaseRequest.getDatabaseAzureRequest().getAzureDatabaseType();
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

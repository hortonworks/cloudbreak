package com.sequenceiq.datalake.service.sdx.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseInstanceTypeV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseInstanceTypesV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;

@Service
public class SdxDatabaseInstanceTypeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDatabaseInstanceTypeService.class);

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    public DatabaseInstanceTypesV4Response listDatabaseInstanceTypes(String environmentCrn, DatabaseCapabilityType databaseType, String architecture) {
        DetailedEnvironmentResponse env = environmentService.getByCrn(environmentCrn);
        String region = env.getLocation().getName();
        String cloudPlatform = env.getCloudPlatform();

        DatabaseCapabilityType resolvedType = resolveDatabaseCapabilityType(databaseType, cloudPlatform);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        PlatformDatabaseCapabilitiesResponse capabilities = ThreadBasedUserCrnProvider.doAs(userCrn,
                () -> environmentPlatformResourceEndpoint.getDatabaseCapabilities(environmentCrn, region, cloudPlatform, null,
                        resolvedType, architecture));

        String defaultInstanceType = capabilities.getRegionDefaultInstances().get(region);
        Set<DatabaseVmTypeResponse> vmTypes = capabilities.getDatabaseVmTypes().getOrDefault(region, Set.of());
        List<DatabaseInstanceTypeV4> filtered = filterByDefaultSpec(vmTypes, defaultInstanceType,
                capabilities.getRegionFallbackInstances().getOrDefault(region, List.of()));

        LOGGER.info("Returning {} curated database instance types for datalake in region {} (default: {})", filtered.size(), region, defaultInstanceType);
        return new DatabaseInstanceTypesV4Response(filtered, defaultInstanceType);
    }

    private DatabaseCapabilityType resolveDatabaseCapabilityType(DatabaseCapabilityType requested, String cloudPlatform) {
        if (requested != null && requested != DatabaseCapabilityType.DEFAULT) {
            return requested;
        }
        if (CloudPlatform.AZURE.name().equalsIgnoreCase(cloudPlatform)) {
            return DatabaseCapabilityType.AZURE_FLEXIBLE;
        }
        return DatabaseCapabilityType.DEFAULT;
    }

    static List<DatabaseInstanceTypeV4> filterByDefaultSpec(Set<DatabaseVmTypeResponse> vmTypes, String defaultInstanceType,
            List<String> fallbackInstanceTypes) {
        if (vmTypes == null || vmTypes.isEmpty()) {
            return buildFallbackList(defaultInstanceType, fallbackInstanceTypes);
        }

        Map<String, Object> defaultProps = findDefaultTypeProperties(vmTypes, defaultInstanceType);
        Integer targetCpu = toInteger(defaultProps.get("Cpu"));
        Float targetMemory = toFloat(defaultProps.get("Memory"));

        if (targetCpu == null || targetMemory == null) {
            LOGGER.warn("Could not determine CPU/memory for default instance type '{}'; returning fallback list", defaultInstanceType);
            return buildFallbackList(defaultInstanceType, fallbackInstanceTypes);
        }

        List<DatabaseInstanceTypeV4> result = collectMatchingTypes(vmTypes, defaultInstanceType, targetCpu, targetMemory);
        result.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return result;
    }

    private static Map<String, Object> findDefaultTypeProperties(Set<DatabaseVmTypeResponse> vmTypes, String defaultInstanceType) {
        for (DatabaseVmTypeResponse vmType : vmTypes) {
            if (vmType.getValue() != null && vmType.getValue().equals(defaultInstanceType) && vmType.getDatabaseVmTypeMetaJson() != null) {
                return vmType.getDatabaseVmTypeMetaJson().getProperties();
            }
        }
        return Map.of();
    }

    private static List<DatabaseInstanceTypeV4> collectMatchingTypes(Set<DatabaseVmTypeResponse> vmTypes, String defaultInstanceType,
            Integer targetCpu, Float targetMemory) {
        List<DatabaseInstanceTypeV4> result = new ArrayList<>();
        for (DatabaseVmTypeResponse vmType : vmTypes) {
            DatabaseVmTypeMetaJson meta = vmType.getDatabaseVmTypeMetaJson();
            if (meta == null) {
                continue;
            }
            Map<String, Object> props = meta.getProperties();
            Integer cpu = toInteger(props.get("Cpu"));
            Float memory = toFloat(props.get("Memory"));
            if (Objects.equals(cpu, targetCpu) && Objects.equals(memory, targetMemory)) {
                String arch = props.get("Architecture") != null ? props.get("Architecture").toString() : null;
                result.add(new DatabaseInstanceTypeV4(vmType.getValue(), cpu, memory, arch,
                        vmType.getValue().equals(defaultInstanceType)));
            }
        }
        return result;
    }

    private static List<DatabaseInstanceTypeV4> buildFallbackList(String defaultInstanceType, List<String> fallbackInstanceTypes) {
        List<DatabaseInstanceTypeV4> result = new ArrayList<>();
        if (defaultInstanceType != null) {
            result.add(new DatabaseInstanceTypeV4(defaultInstanceType, null, null, null, true));
        }
        if (fallbackInstanceTypes != null) {
            for (String fallback : fallbackInstanceTypes) {
                result.add(new DatabaseInstanceTypeV4(fallback, null, null, null, false));
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Float toFloat(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

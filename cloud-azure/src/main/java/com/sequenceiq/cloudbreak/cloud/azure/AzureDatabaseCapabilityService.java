package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.DatabaseAvailabiltyType.databaseAvailabiltyType;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.SAME_ZONE;
import static com.sequenceiq.common.model.AzureHighAvailabiltyMode.ZONE_REDUNDANT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.postgresqlflexibleserver.models.FlexibleServerCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.FlexibleServerEditionCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerSkuCapability;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ZoneRedundantHaSupportedEnum;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRegionProvider;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureCoordinate;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseAvailabiltyType;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@Component
public class AzureDatabaseCapabilityService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseCapabilityService.class);

    @Value("${cb.azure.database.flexible.instanceTypeRegex:}")
    private String instanceTypeRegex;

    @Value("${cb.azure.database.flexible.serverEdition:}")
    private String serverEdition;

    @Value("${cb.azure.database.flexible.defaultInstanceType:}")
    private String defaultInstanceType;

    @Inject
    private AzureClientService azureClientService;

    @Inject
    private AzureRegionProvider azureRegionProvider;

    public PlatformDatabaseCapabilities databaseCapabilities(CloudCredential cloudCredential, Region region, Map<String, String> filters) {
        AzureClient client = azureClientService.getClient(cloudCredential);
        Map<Region, AzureCoordinate> regions = azureRegionProvider.filterEnabledRegions(region);
        LOGGER.debug("Database capabilities will be collected in the following regions: {}", regions);
        Map<DatabaseAvailabiltyType, Collection<Region>> enabledRegions = new HashMap<>();
        enabledRegions.put(databaseAvailabiltyType(SAME_ZONE.name()), getSameZoneSupportedRegions(regions));
        Map<Region, Optional<FlexibleServerCapability>> capabilityMap = client.getFlexibleServerClient().getFlexibleServerCapabilityMap(regions);
        enabledRegions.put(databaseAvailabiltyType(ZONE_REDUNDANT.name()), getZoneRedundantSupportedRegions(regions, capabilityMap));
        return new PlatformDatabaseCapabilities(enabledRegions, getRegionInstanceTypeMap(regions, capabilityMap, filters));
    }

    private Collection<Region> getZoneRedundantSupportedRegions(Map<Region, AzureCoordinate> regions, Map<Region,
            Optional<FlexibleServerCapability>> capabilityMap) {
        Collection<Region> zoneRedundantRegions = new ArrayList<>();
        for (Map.Entry<Region, AzureCoordinate> entry : regions.entrySet()) {
            if (capabilityMap.getOrDefault(entry.getKey(), Optional.empty())
                    .map(capability -> ZoneRedundantHaSupportedEnum.ENABLED.equals(capability.zoneRedundantHaSupported()))
                    .orElse(false)) {
                addRegion(zoneRedundantRegions, entry);
            }
        }
        return zoneRedundantRegions;
    }

    private Collection<Region> getSameZoneSupportedRegions(Map<Region, AzureCoordinate> regions) {
        Collection<Region> sameZoneRegions = new ArrayList<>();
        for (Map.Entry<Region, AzureCoordinate> entry : regions.entrySet()) {
            addRegion(sameZoneRegions, entry);
        }
        return sameZoneRegions;
    }

    private Map<Region, String> getRegionInstanceTypeMap(Map<Region, AzureCoordinate> regions, Map<Region,
            Optional<FlexibleServerCapability>> capabilityMap, Map<String, String> filters) {
        Map<Region, String> instanceTypeMap = new HashMap<>();
        for (Map.Entry<Region, AzureCoordinate> entry : regions.entrySet()) {
            Optional<FlexibleServerCapability> serverCapability = capabilityMap.getOrDefault(entry.getKey(), Optional.empty());
            String instanceType = serverCapability.stream()
                    .map(FlexibleServerCapability::supportedServerEditions)
                    .flatMap(Collection::stream)
                    .filter(this::matchesServerEdition)
                    .flatMap(serverEdition -> serverEdition.supportedServerSkus().stream())
                    .map(ServerSkuCapability::name)
                    .filter(this::matchesInstanceType)
                    .max(Comparator.comparing(this::getInstanceTypeVersion))
                    .orElse(defaultInstanceType);
            putRegion(instanceTypeMap, entry.getKey(), instanceType);
        }
        LOGGER.debug("Default flexible server instancetypes by regions [{}]", instanceTypeMap);
        return instanceTypeMap;
    }

    private boolean matchesServerEdition(FlexibleServerEditionCapability serverEditionCapability) {
        return StringUtils.isEmpty(serverEdition) || serverEdition.equals(serverEditionCapability.name());
    }

    private boolean matchesInstanceType(String instanceType) {
        return StringUtils.isEmpty(instanceTypeRegex) || instanceType.matches(instanceTypeRegex);
    }

    private String getInstanceTypeVersion(String instanceType) {
        return instanceType.substring(Math.max(instanceType.length() - 2, 0));
    }

    private void addRegion(Collection<Region> regions, Map.Entry<Region, AzureCoordinate> entry) {
        com.azure.core.management.Region region = com.azure.core.management.Region.fromName(entry.getKey().getRegionName());
        regions.add(region(region.label()));
        regions.add(region(region.name()));
    }

    private <T> void putRegion(Map<Region, T> regionMap, Region region, T value) {
        com.azure.core.management.Region azureRegion = com.azure.core.management.Region.fromName(region.getRegionName());
        regionMap.put(region(azureRegion.label()), value);
        regionMap.put(region(azureRegion.name()), value);
    }
}

package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.azure.resource.domain.AzureCoordinate;
import com.sequenceiq.cloudbreak.cloud.azure.util.RegionUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Coordinate;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecification;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecifications;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@Component
public class AzureRegionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureRegionProvider.class);

    private static final String ENABLED_REGIONS_FILE = "enabled-regions";

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Value("${cb.arm.zone.parameter.default:North Europe}")
    private String armZoneParameterDefault;

    @Value("${cb.multiaz.azure.availabilityZones:1,2,3}")
    private Set<String> azureAvailabilityZones;

    private Map<Region, AzureCoordinate> enabledRegions = new HashMap<>();

    @PostConstruct
    public void init() {
        enabledRegions = readEnabledRegions();
    }

    public CloudRegions regions(Region region, Collection<com.azure.core.management.Region> azureRegions,
            List<String> entitlements) {
        Map<Region, List<AvailabilityZone>> cloudRegions = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        Map<Region, Coordinate> coordinates = new HashMap<>();
        String defaultRegion = armZoneParameterDefault;
        azureRegions = filterByEnabledRegions(azureRegions);
        List<AvailabilityZone> zones = azureAvailabilityZones.stream()
                .map(e -> availabilityZone(e))
                .collect(Collectors.toList());
        for (com.azure.core.management.Region azureRegion : azureRegions) {
            Coordinate coordinate = enabledRegions.get(region(azureRegion.label()));
            if (isEntitledFor(coordinate, entitlements)) {
                cloudRegions.put(region(azureRegion.label()), zones);
                displayNames.put(region(azureRegion.label()), azureRegion.label());

                if (coordinate == null || coordinate.getLongitude() == null || coordinate.getLatitude() == null) {
                    LOGGER.warn("Unregistered region with location coordinates on azure side: {} using default California", azureRegion.label());
                    coordinates.put(region(azureRegion.label()), Coordinate.defaultCoordinate());
                } else {
                    coordinates.put(region(azureRegion.label()), coordinate);
                }
            }
        }
        if (region != null && !Strings.isNullOrEmpty(region.value())) {
            defaultRegion = region.value();
        }
        return new CloudRegions(cloudRegions, displayNames, coordinates, defaultRegion, true);
    }

    private boolean isEntitledFor(Coordinate coordinate, List<String> entitlements) {
        if (coordinate != null && coordinate.getEntitlements() != null && !coordinate.getEntitlements().isEmpty()) {
            for (String entitlement : coordinate.getEntitlements()) {
                if (!entitlements.contains(entitlement)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Set<com.azure.core.management.Region> filterByEnabledRegions(
            Collection<com.azure.core.management.Region> azureRegions) {
        return azureRegions.stream()
                .filter(reg -> enabledRegions.containsKey(region(reg.label())))
                .collect(Collectors.toSet());
    }

    public Map<Region, AzureCoordinate> enabledRegions() {
        return enabledRegions;
    }

    public Map<Region, AzureCoordinate> filterEnabledRegions(Region... regions) {
        Map<Region, AzureCoordinate> result = enabledRegions;
        List<Region> validRegions = Optional.ofNullable(regions).stream()
                .flatMap(Arrays::stream)
                .filter(region -> region != null && !Strings.isNullOrEmpty(region.value()))
                .toList();
        if (!validRegions.isEmpty()) {
            result = enabledRegions.entrySet()
                    .stream()
                    .filter(entry -> validRegions.stream().anyMatch(region -> entry.getValue().isMatchedRegion(region)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return result;
    }

    private String resourceDefinition() {
        return cloudbreakResourceReaderService.resourceDefinition("azure", ENABLED_REGIONS_FILE);
    }

    private Map<Region, AzureCoordinate> readEnabledRegions() {
        String displayNames = resourceDefinition();
        Map<Region, AzureCoordinate> regionCoordinates = new HashMap<>();
        try {
            RegionCoordinateSpecifications regionCoordinateSpecifications = JsonUtil.readValue(displayNames, RegionCoordinateSpecifications.class);
            for (RegionCoordinateSpecification regionCoordinateSpecification : regionCoordinateSpecifications.getItems()) {
                regionCoordinates.put(region(regionCoordinateSpecification.getName()),
                        AzureCoordinate.AzureCoordinateBuilder.builder()
                                .longitude(regionCoordinateSpecification.getLongitude())
                                .latitude(regionCoordinateSpecification.getLatitude())
                                .displayName(RegionUtil.findByLabelOrName(regionCoordinateSpecification.getName()).label())
                                .key(RegionUtil.findByLabelOrName(regionCoordinateSpecification.getName()).name())
                                .k8sSupported(regionCoordinateSpecification.isK8sSupported())
                                .entitlements(regionCoordinateSpecification.getEntitlements())
                                .build());
            }
        } catch (IOException ignored) {
            LOGGER.error("Failed to read enabled Azure regions from file.");
            return regionCoordinates;
        }
        return regionCoordinates;
    }
}

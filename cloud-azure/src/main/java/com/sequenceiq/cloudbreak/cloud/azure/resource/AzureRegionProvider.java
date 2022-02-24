package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static com.microsoft.azure.management.resources.fluentcore.arm.Region.findByLabelOrName;
import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
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

    private Map<Region, Coordinate> enabledRegions = new HashMap<>();

    @PostConstruct
    public void init() {
        enabledRegions = readEnabledRegions();
    }

    public CloudRegions regions(Region region, Collection<com.microsoft.azure.management.resources.fluentcore.arm.Region> azureRegions,
        List<String> entitlements) {
        Map<Region, List<AvailabilityZone>> cloudRegions = new HashMap<>();
        Map<Region, String> displayNames = new HashMap<>();
        Map<Region, Coordinate> coordinates = new HashMap<>();
        String defaultRegion = armZoneParameterDefault;
        azureRegions = filterByEnabledRegions(azureRegions);
        for (com.microsoft.azure.management.resources.fluentcore.arm.Region azureRegion : azureRegions) {
            Coordinate coordinate = enabledRegions.get(region(azureRegion.label()));
            if (isEntitledFor(coordinate, entitlements)) {
                cloudRegions.put(region(azureRegion.label()), new ArrayList<>());
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

    private Set<com.microsoft.azure.management.resources.fluentcore.arm.Region> filterByEnabledRegions(
            Collection<com.microsoft.azure.management.resources.fluentcore.arm.Region> azureRegions) {
        return azureRegions.stream()
                .filter(reg -> enabledRegions.containsKey(region(reg.label())))
                .collect(Collectors.toSet());
    }

    private String resourceDefinition() {
        return cloudbreakResourceReaderService.resourceDefinition("azure", ENABLED_REGIONS_FILE);
    }

    private Map<Region, Coordinate> readEnabledRegions() {
        String displayNames = resourceDefinition();
        Map<Region, Coordinate> regionCoordinates = new HashMap<>();
        try {
            RegionCoordinateSpecifications regionCoordinateSpecifications = JsonUtil.readValue(displayNames, RegionCoordinateSpecifications.class);
            for (RegionCoordinateSpecification regionCoordinateSpecification : regionCoordinateSpecifications.getItems()) {
                regionCoordinates.put(region(regionCoordinateSpecification.getName()),
                        coordinate(
                                regionCoordinateSpecification.getLongitude(),
                                regionCoordinateSpecification.getLatitude(),
                                findByLabelOrName(regionCoordinateSpecification.getName()).label(),
                                findByLabelOrName(regionCoordinateSpecification.getName()).name(),
                                regionCoordinateSpecification.isK8sSupported(),
                                regionCoordinateSpecification.getEntitlements()));
            }
        } catch (IOException ignored) {
            LOGGER.error("Failed to read enabled Azure regions from file.");
            return regionCoordinates;
        }
        return regionCoordinates;
    }
}

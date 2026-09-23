package com.sequenceiq.environment.environment.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

/**
 * Populates the environment level availability zone list with a provider supplied default on platforms where Cloudera picks the zones instead of the user
 * (currently GCP only). Invoked at environment creation, and from {@code GcpEnvironmentNetworkAzBackfillPatchService} to backfill environments
 * created before the zone list existed (CB-34539).
 */
@Service
public class DefaultAvailabilityZoneService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAvailabilityZoneService.class);

    private static final List<CloudPlatform> POPULATE_DEFAULT_AZ_CLOUD_PLATFORMS = List.of(CloudPlatform.GCP);

    @Inject
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Inject
    private PlatformParameterService platformParameterService;

    @Value("${cb.multiaz.availabilityZones.max:3}")
    private Integer maxAvailabilityZones;

    /**
     * @return true if the environment is on a platform where Cloudera picks the zones and its network has none yet, regardless of whether the default
     * zones can be determined
     */
    public boolean isPopulationNeeded(Environment environment, BaseNetwork network) {
        return getConverter(environment, network)
                .map(converter -> hasNoAvailabilityZones(environment, network, converter))
                .orElse(false);
    }

    public void populateIfMissing(Environment environment, BaseNetwork network, CloudRegions cloudRegions) {
        converterIfPopulationNeeded(environment, network)
                .ifPresent(converter -> updateAvailabilityZones(environment, network, converter, cloudRegions));
    }

    private boolean hasNoAvailabilityZones(Environment environment, BaseNetwork network, EnvironmentNetworkConverter converter) {
        Set<String> existingAvailabilityZones = converter.getAvailabilityZones(network);
        if (!CollectionUtils.isEmpty(existingAvailabilityZones)) {
            LOGGER.debug("Availability zones {} are already present for environment {}", existingAvailabilityZones, environment.getName());
            return false;
        }
        return true;
    }

    /**
     * @return the converter to use when applying default zones, or empty if this environment should not be backfilled
     */
    private Optional<EnvironmentNetworkConverter> converterIfPopulationNeeded(Environment environment, BaseNetwork network) {
        Optional<EnvironmentNetworkConverter> converter = getConverter(environment, network)
                .filter(networkConverter -> hasNoAvailabilityZones(environment, network, networkConverter));
        if (converter.isPresent() && CollectionUtils.isEmpty(environment.getRegionSet())) {
            LOGGER.debug("Environment {} has no region, the default availability zones cannot be determined", environment.getName());
            return Optional.empty();
        }
        return converter;
    }

    /**
     * @return the network converter of the environment, or empty if it has no network, or we don't pick the zones on its platform
     */
    private Optional<EnvironmentNetworkConverter> getConverter(Environment environment, BaseNetwork network) {
        if (network == null) {
            LOGGER.debug("Environment {} has no network, there are no availability zones to populate", environment.getName());
            return Optional.empty();
        }
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        if (!POPULATE_DEFAULT_AZ_CLOUD_PLATFORMS.contains(cloudPlatform)) {
            LOGGER.debug("We don't pick default availability zones on {}, environment {} is skipped", cloudPlatform, environment.getName());
            return Optional.empty();
        }
        EnvironmentNetworkConverter converter = environmentNetworkConverterMap.get(cloudPlatform);
        if (converter == null) {
            LOGGER.warn("There is no network converter for {}, the availability zones of environment {} are left empty",
                    cloudPlatform, environment.getName());
            return Optional.empty();
        }
        return Optional.of(converter);
    }

    private void updateAvailabilityZones(Environment environment, BaseNetwork network, EnvironmentNetworkConverter converter, CloudRegions cloudRegions) {
        String regionName = environment.getRegionSet().iterator().next().getName();
        Set<String> defaultAvailabilityZones = cloudRegions.getCloudRegions()
                .getOrDefault(Region.region(regionName), List.of())
                .stream()
                .map(AvailabilityZone::getValue)
                .sorted()
                .limit(maxAvailabilityZones)
                .collect(Collectors.toSet());
        if (defaultAvailabilityZones.isEmpty()) {
            LOGGER.warn("The cloud provider returned no availability zone for region {} of environment {}, the zone list is left empty",
                    regionName, environment.getName());
            return;
        }
        LOGGER.info("Update Availability zones {} for environment {}", defaultAvailabilityZones, environment.getName());
        converter.updateAvailabilityZones(network, defaultAvailabilityZones);
    }

    public CloudRegions getRegions(Environment environment) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(environment.getCredential());
        platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
        return platformParameterService.getRegionsByCredential(platformResourceRequest, true);
    }
}

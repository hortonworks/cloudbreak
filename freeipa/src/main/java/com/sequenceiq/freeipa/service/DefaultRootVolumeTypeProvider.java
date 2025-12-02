package com.sequenceiq.freeipa.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

/**
 * This class reads the environment properties of default root volume type configurations for each cloud provider platform.
 * In order to configure a root volume type for a platform, eg. GCP, one must specify a property like this:
 * -Dcb.platform.default.rootVolumeType.GCP=SSD
 */
@Service
public class DefaultRootVolumeTypeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRootVolumeTypeProvider.class);

    private static final String DEFAULT_ROOT_VOLUME_TYPE = "HDD";

    private static final String ROOT_VOLUME_TYPE_PROPERTY_PREFIX = "cb.platform.default.rootVolumeType.";

    private Map<String, String> platformVolumeTypeMap = new HashMap<>();

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private Environment environment;

    @PostConstruct
    public void init() {
        PlatformVariants platformVariants = cloudPlatformConnectors.getPlatformVariants();
        platformVolumeTypeMap = Collections.unmodifiableMap(
                platformVariants.getDefaultVariants().keySet()
                .stream()
                .collect(Collectors.toMap(StringType::value, p -> initPlatform(environment, p)))
        );
    }

    public String getForPlatform(String platform) {
        if (!platformVolumeTypeMap.containsKey(platform.toUpperCase(Locale.ROOT))) {
            LOGGER.debug("No default root volume type found for platform: {}. Falling back to default value of {}. "
                            + "Set '{}' property if '{}' is a valid cloud provider.",
                    platform, DEFAULT_ROOT_VOLUME_TYPE, ROOT_VOLUME_TYPE_PROPERTY_PREFIX + platform, platform);
        }
        return platformVolumeTypeMap.getOrDefault(platform.toUpperCase(Locale.ROOT), DEFAULT_ROOT_VOLUME_TYPE);
    }

    private String initPlatform(Environment environment, Platform platform) {
        String propetyKey = ROOT_VOLUME_TYPE_PROPERTY_PREFIX + platform.value();
        if (!environment.containsProperty(propetyKey)) {
            LOGGER.debug("{} property is not set. Defaulting its value to {}.", propetyKey, DEFAULT_ROOT_VOLUME_TYPE);
        }
        return environment.getProperty(propetyKey, DEFAULT_ROOT_VOLUME_TYPE);
    }
}

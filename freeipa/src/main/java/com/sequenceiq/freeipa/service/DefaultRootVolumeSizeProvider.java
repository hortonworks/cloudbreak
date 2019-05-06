package com.sequenceiq.freeipa.service;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

/**
 * This class reads the environment properties of default root volume size configurations for each cloud provider platform.
 * In order to configure a root volume size for a platform, eg. AWS, one must specify a property like this:
 * -Dcb.platform.default.rootVolumeSize.AWS=50
 *
 * For Azure:
 * -Dcb.platform.default.rootVolumeSize.AZURE=100
 *
 * etc.
 *
 */
@Service
public class DefaultRootVolumeSizeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRootVolumeSizeProvider.class);

    private static final Integer DEFAULT_ROOT_VOLUME_SIZE = 50;

    private static final String ROOT_VOLUME_SIZE_PROPERTY_PREFIX = "cb.platform.default.rootVolumeSize.";

    private final Map<String, Integer> platformVolumeSizeMap;

    public DefaultRootVolumeSizeProvider(CloudPlatformConnectors cloudPlatformConnectors, Environment environment) {
        PlatformVariants platformVariants = cloudPlatformConnectors.getPlatformVariants();
        platformVolumeSizeMap = Collections.unmodifiableMap(
                platformVariants.getDefaultVariants().keySet()
                .stream()
                .collect(Collectors.toMap(StringType::value, p -> initPlatform(environment, p)))
        );
    }

    public int getForPlatform(String platform) {
        if (!platformVolumeSizeMap.containsKey(platform.toUpperCase())) {
            LOGGER.debug("No default root volume size found for platform: {}. Falling back to default value of 50 GB. "
                            + "Set '{}' property if '{}' is a valid cloud provider.",
                    platform, ROOT_VOLUME_SIZE_PROPERTY_PREFIX + platform, platform);
        }
        return platformVolumeSizeMap.getOrDefault(platform.toUpperCase(), DEFAULT_ROOT_VOLUME_SIZE);
    }

    private Integer initPlatform(Environment environment, Platform platform) {
        String propetyKey = ROOT_VOLUME_SIZE_PROPERTY_PREFIX + platform.value();
        if (!environment.containsProperty(propetyKey)) {
            LOGGER.debug("{} property is not set. Defaulting its value to 50.", propetyKey);
        }
        return Integer.valueOf(environment.getProperty(propetyKey, DEFAULT_ROOT_VOLUME_SIZE.toString()));
    }
}

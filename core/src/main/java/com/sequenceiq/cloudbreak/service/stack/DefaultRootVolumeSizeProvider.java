package com.sequenceiq.cloudbreak.service.stack;

import java.util.Collections;
import java.util.Locale;
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
 */
@Service
public class DefaultRootVolumeSizeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRootVolumeSizeProvider.class);

    private static final Integer DEFAULT_ROOT_VOLUME_SIZE = 200;

    private static final Integer GATEWAY_DEFAULT_ROOT_VOLUME_SIZE = 300;

    private static final String ROOT_VOLUME_SIZE_PROPERTY_PREFIX = "cb.platform.default.rootVolumeSize.";

    private static final String GATEWAY_ROOT_VOLUME_SIZE_PROPERTY_PREFIX = "cb.platform.default.gatewayRootVolumeSize.";

    private final Map<String, Integer> platformVolumeSizeMap;

    private final Map<String, Integer> platformGatewayVolumeSizeMap;

    public DefaultRootVolumeSizeProvider(CloudPlatformConnectors cloudPlatformConnectors, Environment environment) {
        PlatformVariants platformVariants = cloudPlatformConnectors.getPlatformVariants();
        platformVolumeSizeMap = Collections.unmodifiableMap(
                platformVariants.getDefaultVariants().keySet()
                        .stream()
                        .collect(Collectors.toMap(StringType::value, platform ->
                                getDefaultVolumeSizeValue(environment, platform, ROOT_VOLUME_SIZE_PROPERTY_PREFIX, DEFAULT_ROOT_VOLUME_SIZE)))
        );
        platformGatewayVolumeSizeMap = Collections.unmodifiableMap(
                platformVariants.getDefaultVariants().keySet()
                        .stream()
                        .collect(Collectors.toMap(StringType::value, platform ->
                                getDefaultVolumeSizeValue(environment, platform, GATEWAY_ROOT_VOLUME_SIZE_PROPERTY_PREFIX, GATEWAY_DEFAULT_ROOT_VOLUME_SIZE)))
        );
    }

    public int getDefaultRootVolumeForPlatform(String platform, boolean gatewayType) {
        String upperPlatform = platform.toUpperCase(Locale.ROOT);

        if (gatewayType) {
            if (!platformGatewayVolumeSizeMap.containsKey(upperPlatform)) {
                logDefaultSizeUsage(platform, GATEWAY_DEFAULT_ROOT_VOLUME_SIZE, GATEWAY_ROOT_VOLUME_SIZE_PROPERTY_PREFIX);
            }
            return platformGatewayVolumeSizeMap.getOrDefault(upperPlatform, GATEWAY_DEFAULT_ROOT_VOLUME_SIZE);
        } else {
            if (!platformVolumeSizeMap.containsKey(upperPlatform)) {
                logDefaultSizeUsage(platform, DEFAULT_ROOT_VOLUME_SIZE, ROOT_VOLUME_SIZE_PROPERTY_PREFIX);
            }
            return platformVolumeSizeMap.getOrDefault(upperPlatform, DEFAULT_ROOT_VOLUME_SIZE);
        }
    }

    private void logDefaultSizeUsage(String platform, int defaultSize, String propertyPrefix) {
        LOGGER.debug("No default root volume size found for platform: {}. " +
                        "Falling back to default value of {} GB. " +
                        "Set '{}' property if '{}' is a valid cloud provider.",
                platform, defaultSize, propertyPrefix + platform, platform);
    }

    private Integer getDefaultVolumeSizeValue(Environment environment, Platform platform,
            String propertyPrefix, Integer defaultSize) {
        String propertyKey = propertyPrefix + platform.value();
        if (!environment.containsProperty(propertyKey)) {
            LOGGER.debug("{} property is not set. Defaulting its value to {}", propertyKey, defaultSize);
        }
        return Integer.valueOf(environment.getProperty(propertyKey, defaultSize.toString()));
    }
}

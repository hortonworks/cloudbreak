package com.sequenceiq.freeipa.service.stack.instance;

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
 * This class reads the environment properties of default instance type configurations for each cloud provider platform.
 * In order to configure a instance type for a platform, eg. AWS, one must specify a property like this:
 * -Dfreeipa.platform.default.instanceType.AWS=m5.large
 *
 * For Azure:
 * -Dfreeipa.platform.default.instanceType.AZURE=Standard_DS3_v2
 *
 * etc.
 *
 */
@Service
public class DefaultInstanceTypeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultInstanceTypeProvider.class);

    private static final String DEFAULT_INSTANCE_TYPE_PROPERTY_PERFIX = "freeipa.platform.default.instanceType.";

    private final Map<String, String> platformInstanceTypeMap;

    public DefaultInstanceTypeProvider(CloudPlatformConnectors cloudPlatformConnectors, Environment environment) {
        PlatformVariants platformVariants = cloudPlatformConnectors.getPlatformVariants();
        platformInstanceTypeMap = Collections.unmodifiableMap(
                platformVariants.getDefaultVariants().keySet()
                .stream()
                .collect(Collectors.toMap(StringType::value, p -> initPlatform(environment, p)))
        );
    }

    public String getForPlatform(String platform) {
        if (!platformInstanceTypeMap.containsKey(platform.toUpperCase(Locale.ROOT))) {
            LOGGER.debug("No default instance type found for platform: {}. Falling back to default empty string. "
                            + "Set '{}' property if '{}' is a valid cloud provider.",
                    platform, DEFAULT_INSTANCE_TYPE_PROPERTY_PERFIX + platform, platform);
        }
        return platformInstanceTypeMap.getOrDefault(platform.toUpperCase(Locale.ROOT), "");
    }

    private String initPlatform(Environment environment, Platform platform) {
        String propetyKey = DEFAULT_INSTANCE_TYPE_PROPERTY_PERFIX + platform.value();
        if (!environment.containsProperty(propetyKey)) {
            LOGGER.debug("{} property is not set. Defaulting its value to empty string.", propetyKey);
        }
        return environment.getProperty(propetyKey, "");
    }
}

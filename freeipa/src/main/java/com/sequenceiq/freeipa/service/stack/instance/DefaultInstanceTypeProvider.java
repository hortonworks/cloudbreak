package com.sequenceiq.freeipa.service.stack.instance;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.common.model.Architecture;

/**
 * This class reads the environment properties of default instance type configurations for each cloud provider platform.
 * In order to configure a instance type for a platform, eg. AWS, one must specify a property like this:
 * -Dfreeipa.platform.default.instanceType.AWS=m5.large
 * <p>
 * For Azure:
 * -Dfreeipa.platform.default.instanceType.AZURE=Standard_DS3_v2
 * <p>
 * etc.
 */
@Service
public class DefaultInstanceTypeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultInstanceTypeProvider.class);

    private static final String DEFAULT_INSTANCE_TYPE_PROPERTY_PERFIX = "freeipa.platform.default.instanceType.";

    private Map<String, Map<Architecture, String>> platformInstanceTypeMap = new HashMap<>();

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private Environment environment;

    @PostConstruct
    public void init() {
        PlatformVariants platformVariants = cloudPlatformConnectors.getPlatformVariants();
        Map<String, Map<Architecture, String>> instanceMap = new HashMap<>();
        platformVariants.getDefaultVariants().keySet().forEach(platform -> {
            instanceMap.put(platform.value(), new HashMap<>());
            String x86Instance = initPlatform(environment, platform, Architecture.X86_64);
            if (StringUtils.isNotBlank(x86Instance)) {
                instanceMap.get(platform.value()).put(Architecture.X86_64, x86Instance);
            }
            String arm64Instance = initPlatform(environment, platform, Architecture.ARM64);
            if (StringUtils.isNotBlank(arm64Instance)) {
                instanceMap.get(platform.value()).put(Architecture.ARM64, arm64Instance);
            }
            if (StringUtils.isAllBlank(x86Instance, arm64Instance)) {
                instanceMap.get(platform.value()).put(Architecture.X86_64, initPlatform(environment, platform, null));
            }
        });
        platformInstanceTypeMap = Collections.unmodifiableMap(instanceMap);
    }

    public String getForPlatform(String platform, Architecture architecture) {
        if (!platformInstanceTypeMap.containsKey(platform.toUpperCase(Locale.ROOT))) {
            LOGGER.debug("No default instance type found for platform: {}. Falling back to default empty string. "
                            + "Set '{}' property if '{}' is a valid cloud provider.",
                    platform, DEFAULT_INSTANCE_TYPE_PROPERTY_PERFIX + platform, platform);
        }
        return platformInstanceTypeMap.getOrDefault(platform.toUpperCase(Locale.ROOT), Collections.emptyMap())
                .getOrDefault(Optional.ofNullable(architecture).orElse(Architecture.X86_64), "");
    }

    private String initPlatform(Environment environment, Platform platform, Architecture architecture) {
        String propetyKey = DEFAULT_INSTANCE_TYPE_PROPERTY_PERFIX + platform.value() + (architecture == null ? "" : ('.' + architecture.getName()));
        if (!environment.containsProperty(propetyKey)) {
            LOGGER.debug("{} property is not set. Defaulting its value to empty string.", propetyKey);
        }
        return environment.getProperty(propetyKey, "");
    }
}

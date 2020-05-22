package com.sequenceiq.it.cloudbreak.util.spot;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.config.SpotProperties;

@Component
public class SpotUtil {

    private static final ThreadLocal<Boolean> USE_SPOT_INSTANCES = new ThreadLocal<>();

    private final SpotProperties spotProperties;

    private final CommonCloudProperties commonCloudProperties;

    public SpotUtil(SpotProperties spotProperties, CommonCloudProperties commonCloudProperties) {
        this.spotProperties = spotProperties;
        this.commonCloudProperties = commonCloudProperties;
    }

    public boolean shouldUseSpotInstancesForTest(Method testMethod) {
        return spotIsEnabledOnCurrentCloudPlatform()
                && testMethod.isAnnotationPresent(UseSpotInstances.class);
    }

    private boolean spotIsEnabledOnCurrentCloudPlatform() {
        return spotProperties.getEnabledCloudPlatforms().contains(commonCloudProperties.getCloudProvider());
    }

    public void setUseSpotInstances(Boolean useSpotInstances) {
        USE_SPOT_INSTANCES.set(useSpotInstances);
    }

    public boolean isUseSpotInstances() {
        return Optional.ofNullable(USE_SPOT_INSTANCES.get()).orElse(Boolean.FALSE);
    }
}

package com.sequenceiq.it.cloudbreak.util.spot;

import java.lang.reflect.Method;
import java.util.Optional;

public class SpotUtil {

    private static final ThreadLocal<Boolean> USE_SPOT_INSTANCES = new ThreadLocal<>();

    private SpotUtil() {

    }

    public static boolean shouldUseSpotInstances(Method testMethod) {
        return testMethod.isAnnotationPresent(UseSpotInstances.class);
    }

    public static void useSpotInstances(Boolean useSpotInstances) {
        USE_SPOT_INSTANCES.set(useSpotInstances);
    }

    public static int getSpotPercentage() {
        return Optional.ofNullable(USE_SPOT_INSTANCES.get()).orElse(Boolean.FALSE) ? 100 : 0;
    }
}

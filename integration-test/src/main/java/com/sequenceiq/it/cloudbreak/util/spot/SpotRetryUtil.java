package com.sequenceiq.it.cloudbreak.util.spot;

import java.util.HashSet;
import java.util.Set;

import org.testng.ITestNGMethod;

public class SpotRetryUtil {

    private static final Set<ITestNGMethod> RETRIED_TEST_METHODS = new HashSet<>();

    private SpotRetryUtil() {

    }

    public static boolean willRetry(ITestNGMethod testMethod) {
        return SpotUtil.shouldUseSpotInstances(testMethod.getConstructorOrMethod().getMethod())
                && RETRIED_TEST_METHODS.add(testMethod);
    }

    public static boolean isRetried(ITestNGMethod testMethod) {
        return RETRIED_TEST_METHODS.contains(testMethod);
    }
}

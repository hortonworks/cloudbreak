package com.sequenceiq.it.cloudbreak.util.spot;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.testng.ITestNGMethod;

@Component
public class SpotRetryUtil {

    private static final Set<ITestNGMethod> RETRIED_TEST_METHODS = new HashSet<>();

    private final SpotUtil spotUtil;

    public SpotRetryUtil(SpotUtil spotUtil) {
        this.spotUtil = spotUtil;
    }

    public boolean willRetry(ITestNGMethod testMethod) {
        return spotUtil.shouldUseSpotInstancesForTest(testMethod.getConstructorOrMethod().getMethod())
                && RETRIED_TEST_METHODS.add(testMethod);
    }

    public boolean isRetried(ITestNGMethod testMethod) {
        return RETRIED_TEST_METHODS.contains(testMethod);
    }
}

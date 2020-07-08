package com.sequenceiq.it.cloudbreak.util.spot;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.testng.ITestNGMethod;

@Component
public class SpotRetryUtil {

    private static final Set<ITestNGMethod> RETRIED_TEST_METHODS = new HashSet<>();

    private final SpotUtil spotUtil;

    private final boolean retryEnabled;

    public SpotRetryUtil(SpotUtil spotUtil, @Value("${integrationtest.spot.retryEnabled:false}") boolean retryEnabled) {
        this.spotUtil = spotUtil;
        this.retryEnabled = retryEnabled;
    }

    public boolean willRetry(ITestNGMethod testMethod) {
        return retryEnabled
                && spotUtil.shouldUseSpotInstancesForTest(testMethod.getConstructorOrMethod().getMethod())
                && RETRIED_TEST_METHODS.add(testMethod);
    }

    public boolean isRetried(ITestNGMethod testMethod) {
        return RETRIED_TEST_METHODS.contains(testMethod);
    }
}

package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.function.Function;

import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.QuartzThreadUtil;

public class AzureQuartzRetryUtils {

    private static final Integer MAX_CLIENT_QUARTZ_RETRY = 0;

    private AzureQuartzRetryUtils() {

    }

    public static void reconfigureAzureClientIfNeeded(Function<Integer, Object> azureClientFunction) {
        if (QuartzThreadUtil.isCurrentQuartzThread()) {
            azureClientFunction.apply(MAX_CLIENT_QUARTZ_RETRY);
        }
    }

    public static void reconfigureHttpClientRetryOptionsIfNeeded(Function<RetryOptions, Object> httpClientFunction) {
        if (QuartzThreadUtil.isCurrentQuartzThread()) {
            httpClientFunction.apply(getQuartzRetryOptions());
        }
    }

    public static void reconfigureHttpClientRetryPolicyIfNeeded(Function<RetryPolicy, Object> httpClientFunction) {
        if (QuartzThreadUtil.isCurrentQuartzThread()) {
            httpClientFunction.apply(getQuartzRetryPolicy());
        }
    }

    private static RetryPolicy getQuartzRetryPolicy() {
        return new RetryPolicy(getQuartzRetryOptions());
    }

    private static RetryOptions getQuartzRetryOptions() {
        ExponentialBackoffOptions exponentialBackoffOptions = new ExponentialBackoffOptions();
        exponentialBackoffOptions.setMaxRetries(MAX_CLIENT_QUARTZ_RETRY);
        return new RetryOptions(exponentialBackoffOptions);
    }
}

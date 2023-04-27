package com.sequenceiq.cloudbreak.cloud.azure.client;

import static com.sequenceiq.cloudbreak.quartz.configuration.SchedulerFactoryConfig.QUARTZ_EXECUTOR_THREAD_NAME_PREFIX;

import java.util.function.Function;

import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;

public class AzureQuartzRetryUtils {

    private static final Integer MAX_CLIENT_QUARTZ_RETRY = 0;

    private AzureQuartzRetryUtils() {

    }

    public static void reconfigureAzureClientIfNeeded(Function<Integer, Object> azureClientFunction) {
        String threadName = Thread.currentThread().getName();
        if (threadName.contains(QUARTZ_EXECUTOR_THREAD_NAME_PREFIX)) {
            azureClientFunction.apply(MAX_CLIENT_QUARTZ_RETRY);
        }
    }

    public static void reconfigureHttpClientIfNeeded(Function<RetryOptions, Object> httpClientFunction) {
        String threadName = Thread.currentThread().getName();
        if (threadName.contains(QUARTZ_EXECUTOR_THREAD_NAME_PREFIX)) {
            httpClientFunction.apply(getQuartzRetryOptions());
        }
    }

    private static RetryOptions getQuartzRetryOptions() {
        ExponentialBackoffOptions exponentialBackoffOptions = new ExponentialBackoffOptions();
        exponentialBackoffOptions.setMaxRetries(MAX_CLIENT_QUARTZ_RETRY);
        return new RetryOptions(exponentialBackoffOptions);
    }
}

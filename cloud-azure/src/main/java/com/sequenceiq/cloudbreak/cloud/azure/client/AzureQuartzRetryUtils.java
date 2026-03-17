package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.time.Duration;
import java.util.function.Function;

import com.azure.core.http.HttpClient;
import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.QuartzThreadUtil;

public class AzureQuartzRetryUtils {
    private static final Integer MAX_CLIENT_QUARTZ_RETRY = 0;

    private static final Duration QUARTZ_CONNECTION_TIMEOUT = Duration.ofSeconds(10L);

    private static final Duration QUARTZ_READ_TIMEOUT = Duration.ofSeconds(10L);

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

    public static void reconfigureHttpClientIfNeeded(Function<RetryPolicy, Object> retryPolicySetter, Function<HttpClient, Object> httpClientSetter,
            OkHttpAsyncHttpClientBuilder httpClientBuilder) {
        if (QuartzThreadUtil.isCurrentQuartzThread()) {
            retryPolicySetter.apply(getQuartzRetryPolicy());
            httpClientSetter.apply(reconfigureHttpClient(httpClientBuilder).build());
        } else {
            httpClientSetter.apply(httpClientBuilder.build());
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

    private static OkHttpAsyncHttpClientBuilder reconfigureHttpClient(OkHttpAsyncHttpClientBuilder httpClientBuilder) {
        return httpClientBuilder
                .connectionTimeout(QUARTZ_CONNECTION_TIMEOUT)
                .readTimeout(QUARTZ_READ_TIMEOUT);
    }
}

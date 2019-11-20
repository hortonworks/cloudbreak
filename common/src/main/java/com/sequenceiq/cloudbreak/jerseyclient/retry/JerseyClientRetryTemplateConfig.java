package com.sequenceiq.cloudbreak.jerseyclient.retry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class JerseyClientRetryTemplateConfig {

    private static final int INITIAL_BACKOFF_IN_MILLIS = 3_000;

    private static final int MAX_BACKOFF_IN_MILLIS = 60 * 1000;

    @Bean
    public RetryTemplate jerseyClientRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(INITIAL_BACKOFF_IN_MILLIS);
        exponentialBackOffPolicy.setMultiplier(2.0);
        exponentialBackOffPolicy.setMaxInterval(MAX_BACKOFF_IN_MILLIS);
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
        retryTemplate.setRetryPolicy(new JerseyClientRetryPolicy());
        return retryTemplate;
    }

}

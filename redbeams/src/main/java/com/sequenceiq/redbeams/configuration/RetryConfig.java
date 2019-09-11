package com.sequenceiq.redbeams.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Configures retry logic for redbeams.
 */
@Configuration
public class RetryConfig {

    /**
     * The maximum number of attempts to make when calling another Cloudbreak
     * service.
     */
    @Value("${redbeams.retry.cb.maxAttempts:3}")
    private int cbMaxAttempts;

    /**
     * The fixed backoff period, in milliseconds, for retries of calls to other
     * Cloudbreak services.
     */
    @Value("${redbeams.retry.cb.backOffPeriodInMs:5000}")
    private long cbBackOffPeriodInMs;

    /**
     * Defines the retry template to use for calls to other Cloudbreak services.
     *
     * @return retry template
     */
    @Bean
    public RetryTemplate cbRetryTemplate() {
        RetryTemplate t = new RetryTemplate();

        t.setRetryPolicy(new SimpleRetryPolicy(cbMaxAttempts));
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(cbBackOffPeriodInMs);
        t.setBackOffPolicy(backOffPolicy);

        return t;
    }

}

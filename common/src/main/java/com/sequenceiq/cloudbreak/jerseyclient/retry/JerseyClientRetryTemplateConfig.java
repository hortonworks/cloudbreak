package com.sequenceiq.cloudbreak.jerseyclient.retry;

import java.time.Duration;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class JerseyClientRetryTemplateConfig {

    @Inject
    private JerseyClientRetryProperties jerseyClientRetryProperties;

    @Bean
    public RetryTemplate jerseyClientRetryTemplate() {
        return createRetryTemplate(jerseyClientRetryProperties.getDefaultInitialBackoffDuration(), jerseyClientRetryProperties.getDefaultMaxBackoffDuration(),
                jerseyClientRetryProperties.getDefaultMultiplier());
    }

    @Bean
    public RetryTemplate quartzJerseyClientRetryTemplate() {
        return createRetryTemplate(jerseyClientRetryProperties.getQuartzInitialBackoffDuration(), jerseyClientRetryProperties.getQuartzMaxBackoffDuration(),
                jerseyClientRetryProperties.getQuartzMultiplier());
    }

    private RetryTemplate createRetryTemplate(Duration initialInterval, Duration maxInterval, double multiplier) {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(initialInterval.toMillis());
        exponentialBackOffPolicy.setMultiplier(multiplier);
        exponentialBackOffPolicy.setMaxInterval(maxInterval.toMillis());
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
        retryTemplate.setRetryPolicy(new JerseyClientRetryPolicy());
        return retryTemplate;
    }

}

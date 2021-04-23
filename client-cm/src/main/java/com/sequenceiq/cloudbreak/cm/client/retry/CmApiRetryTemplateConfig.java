package com.sequenceiq.cloudbreak.cm.client.retry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class CmApiRetryTemplateConfig {

    private static final int BACK_OFF_PERIOD = 5000;

    @Bean
    public RetryTemplate cmApiRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(BACK_OFF_PERIOD);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        retryTemplate.setRetryPolicy(new ApiExceptionRetryPolicy());
        retryTemplate.setThrowLastExceptionOnExhausted(true);
        return retryTemplate;
    }

}

package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import javax.inject.Inject;

import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.config.GrpcCcmV2Config;

@Component
public class RetryTemplateFactory {

    @Inject
    private GrpcCcmV2Config grpcCcmV2Config;

    public RetryTemplate getRetryTemplate() {
        TimeoutRetryPolicy policy = new TimeoutRetryPolicy();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();

        policy.setTimeout(grpcCcmV2Config.getTimeoutMs());
        fixedBackOffPolicy.setBackOffPeriod(grpcCcmV2Config.getPollingIntervalMs());

        RetryTemplate template = new RetryTemplate();
        template.setBackOffPolicy(fixedBackOffPolicy);
        template.setRetryPolicy(policy);
        template.setThrowLastExceptionOnExhausted(true);
        return template;
    }
}

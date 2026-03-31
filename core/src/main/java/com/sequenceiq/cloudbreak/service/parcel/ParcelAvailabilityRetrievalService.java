package com.sequenceiq.cloudbreak.service.parcel;

import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;

@Component
public class ParcelAvailabilityRetrievalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelAvailabilityRetrievalService.class);

    private static final int MAX_ATTEMPTS = 3;

    @Value("${cb.parcel.retry.backOffDelay:2000}")
    private long backOffDelay;

    @Inject
    private RestClientFactory restClientFactory;

    @Inject
    private PaywallCredentialPopulator paywallCredentialPopulator;

    private RetryTemplate retryTemplate;

    @PostConstruct
    void initRetryTemplate() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(backOffDelay);
        RetryTemplate template = new RetryTemplate();
        template.setBackOffPolicy(backOffPolicy);
        template.setRetryPolicy(new SimpleRetryPolicy(MAX_ATTEMPTS, Map.<Class<? extends Throwable>, Boolean>of(ProcessingException.class, true)));
        retryTemplate = template;
    }

    @Cacheable(cacheNames = "parcelAvailabilityCache", key = "#url")
    public Response getHeadResponseForParcel(String url) {
        if (PaywallCredentialPopulator.ARCHIVE_URL_PATTERN.matcher(url).find()) {
            LOGGER.info("Archive URL '{}' detected, HEAD request will be attempted up to {} times on ProcessingException", url, MAX_ATTEMPTS);
            return retryTemplate.execute(ctx -> doHeadRequest(url));
        }
        LOGGER.info("Non-archive URL '{}', HEAD request will be sent without retry", url);
        return doHeadRequest(url);
    }

    private Response doHeadRequest(String url) {
        Client client = restClientFactory.getOrCreateWithFollowRedirects();
        WebTarget target = client.target(url);
        paywallCredentialPopulator.populateWebTarget(url, target);
        Response response = target.request().head();
        LOGGER.info("HEAD request for {} returned status: {}", url, response.getStatus());
        return response;
    }
}

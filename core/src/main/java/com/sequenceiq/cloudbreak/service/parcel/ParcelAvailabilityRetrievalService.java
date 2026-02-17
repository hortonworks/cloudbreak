package com.sequenceiq.cloudbreak.service.parcel;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;

@Component
public class ParcelAvailabilityRetrievalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParcelAvailabilityRetrievalService.class);

    @Value("#{'${cb.http.retryableStatusCodes:}'.split(',')}")
    private List<Integer> retryableHttpCodes;

    @Value("${cb.parcel.retry.maxAttempts:5}")
    private Integer retryMaxAttempts;

    @Inject
    private RestClientFactory restClientFactory;

    @Inject
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Retryable(retryFor = Exception.class,
            maxAttemptsExpression = "${cb.parcel.retry.maxAttempts:5}",
            backoff = @Backoff(delayExpression = "${cb.parcel.retry.backOffDelay:2000}",
                    multiplierExpression = "${cb.parcel.retry.backOffMultiplier:2}"))
    @Cacheable(cacheNames = "parcelAvailabilityCache", key = "#url")
    public Response getHeadResponseForParcel(String url) {
        Client client = restClientFactory.getOrCreateWithFollowRedirects();
        WebTarget target = client.target(url);
        paywallCredentialPopulator.populateWebTarget(url, target);
        Response response = target.request().head();
        LOGGER.info("Head request for {} status: {}", url, response.getStatus());
        if (retryableHttpCodes.contains(response.getStatus())) {
            if (RetrySynchronizationManager.getContext().getRetryCount() < retryMaxAttempts - 1) {
                LOGGER.info("Retry for Http Status {}", response.getStatus());
                throw new RuntimeException(String.format("Got Http Status %s", response.getStatus()));
            } else {
                LOGGER.info("Retry exhausted");
            }
        }
        return response;
    }
}

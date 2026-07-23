package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.io.IOException;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;

@Service
public class GcpInstanceRetrievalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceRetrievalService.class);

    @Inject
    private GcpStackUtil gcpStackUtil;

    /**
     * Fetches a single GCP compute instance, retrying up to 5 times on transient
     * {@link GoogleJsonResponseException} (GCP HTTP errors). This lives in its own bean so the Spring retry proxy
     * actually intercepts the call; other {@link IOException}s fail fast without retrying.
     */
    @Retryable(value = GoogleJsonResponseException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public Instance getInstance(Compute compute, String projectId, String zone, String instanceId) throws IOException {
        LOGGER.debug("Fetching GCP instance {} in project {} zone {}.", instanceId, projectId, zone);
        return gcpStackUtil.getComputeInstanceWithId(compute, projectId, zone, instanceId);
    }
}

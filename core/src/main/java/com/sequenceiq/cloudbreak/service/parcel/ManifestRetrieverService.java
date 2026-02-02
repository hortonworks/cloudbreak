package com.sequenceiq.cloudbreak.service.parcel;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;

@Component
public class ManifestRetrieverService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestRetrieverService.class);

    private static final int MANIFEST_READ_TIMEOUT_IN_MS = 5000;

    private static final int MAX_RETRIES = 3;

    @Inject
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Inject
    private RestClientFactory restClientFactory;

    @Cacheable(cacheNames = "parcelMetadataCache", key = "#baseUrl")
    @Retryable(value = ProcessingException.class, maxAttempts = MAX_RETRIES,
            backoff = @Backoff(delay = MANIFEST_READ_TIMEOUT_IN_MS))
    public ImmutablePair<ManifestStatus, Manifest> readRepoManifest(String baseUrl) throws CloudbreakRuntimeException {
        String manifestUrl = StringUtils.stripEnd(baseUrl, "/") + "/manifest.json";
        try {
            Client client = restClientFactory.getOrCreateDefault();
            client.property(ClientProperties.CONNECT_TIMEOUT, MANIFEST_READ_TIMEOUT_IN_MS);
            client.property(ClientProperties.READ_TIMEOUT, MANIFEST_READ_TIMEOUT_IN_MS);
            LOGGER.debug("Trying to retrieve manifest {}", manifestUrl);
            WebTarget target = client.target(manifestUrl);
            addPaywallCredentialsIfNecessary(baseUrl, target);
            Response response = target.request().get();
            Manifest manifest = readResponse(target, response);
            return ImmutablePair.of(ManifestStatus.SUCCESS, manifest);
        } catch (ProcessingException e) {
            LOGGER.warn("Attempt to read manifest.json from parcel repo '{}' failed. Retrying...", manifestUrl, e);
            throw e;
        } catch (JsonParseException e) {
            LOGGER.warn("Could not parse manifest.json: {}, message: {}", manifestUrl, e.getMessage());
            return ImmutablePair.of(ManifestStatus.COULD_NOT_PARSE, null);
        } catch (Exception e) {
            LOGGER.warn("Could not read manifest.json from parcel repo: {}, message: {}", manifestUrl, e.getMessage());
            return ImmutablePair.of(ManifestStatus.FAILED, null);
        }
    }

    @Recover
    public ImmutablePair<ManifestStatus, Manifest> recoverRepoManifest(ProcessingException e, String baseUrl) throws CloudbreakRuntimeException {
        String manifestUrl = StringUtils.stripEnd(baseUrl, "/") + "/manifest.json";
        String message = String.format("Could not read manifest.json from parcel repo '%s' after %d attempts.", manifestUrl, MAX_RETRIES);
        LOGGER.warn(message, e);
        throw new CloudbreakRuntimeException(message, e);
    }

    private void addPaywallCredentialsIfNecessary(String baseUrl, WebTarget target) {
        paywallCredentialPopulator.populateWebTarget(baseUrl, target);
    }

    private Manifest readResponse(WebTarget target, Response response) throws IOException {
        if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
            throw new RuntimeException(String.format("Failed to get manifest.json from '%s' due to: '%s'",
                    target.getUri().toString(), response.getStatusInfo().getReasonPhrase()));
        } else {
            return JsonUtil.readValue(response.readEntity(String.class), Manifest.class);
        }
    }
}

package com.sequenceiq.cloudbreak.service.parcel;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;

@Component
public class ManifestRetrieverService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestRetrieverService.class);

    private static final int MANIFEST_READ_TIMEOUT = 5000;

    @Inject
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Inject
    private RestClientFactory restClientFactory;

    @Cacheable(cacheNames = "parcelMetadataCache", key = "#baseUrl")
    public ImmutablePair<ManifestStatus, Manifest> readRepoManifest(String baseUrl) {
        Manifest manifest = null;
        try {
            Client client = restClientFactory.getOrCreateDefault();
            client.property(ClientProperties.CONNECT_TIMEOUT, MANIFEST_READ_TIMEOUT);
            client.property(ClientProperties.READ_TIMEOUT, MANIFEST_READ_TIMEOUT);
            WebTarget target = client.target(StringUtils.stripEnd(baseUrl, "/") + "/manifest.json");
            addPaywallCredentialsIfNecessary(baseUrl, target);
            Response response = target.request().get();
            manifest = readResponse(target, response);
            return ImmutablePair.of(ManifestStatus.SUCCESS, manifest);
        } catch (ProcessingException e) {
            LOGGER.info("Could not parse manifest.json: {}, message: {}", manifest, e.getMessage());
            return ImmutablePair.of(ManifestStatus.COULD_NOT_PARSE, null);
        } catch (Exception e) {
            LOGGER.info("Could not read manifest.json from parcel repo: {}, message: {}", baseUrl, e.getMessage());
            return ImmutablePair.of(ManifestStatus.FAILED, null);
        }
    }

    private void addPaywallCredentialsIfNecessary(String baseUrl, WebTarget target) {
        paywallCredentialPopulator.populateWebTarget(baseUrl, target);
    }

    private Manifest readResponse(WebTarget target, Response response) {
        if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
            throw new RuntimeException(String.format("Failed to get manifest.json from '%s' due to: '%s'",
                    target.getUri().toString(), response.getStatusInfo().getReasonPhrase()));
        } else {
            return response.readEntity(Manifest.class);
        }
    }
}

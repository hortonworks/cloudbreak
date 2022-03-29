package com.sequenceiq.cloudbreak.service.parcel;

import java.io.IOException;

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

import com.fasterxml.jackson.core.JsonParseException;
import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

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
        String manifestUrl = StringUtils.stripEnd(baseUrl, "/") + "/manifest.json";
        try {
            Client client = restClientFactory.getOrCreateDefault();
            client.property(ClientProperties.CONNECT_TIMEOUT, MANIFEST_READ_TIMEOUT);
            client.property(ClientProperties.READ_TIMEOUT, MANIFEST_READ_TIMEOUT);
            LOGGER.debug("Trying to retrieve manifest {}", manifestUrl);
            WebTarget target = client.target(manifestUrl);
            addPaywallCredentialsIfNecessary(baseUrl, target);
            Response response = target.request().get();
            Manifest manifest = readResponse(target, response);
            return ImmutablePair.of(ManifestStatus.SUCCESS, manifest);
        } catch (ProcessingException | JsonParseException e) {
            LOGGER.warn("Could not parse manifest.json: {}, message: {}", manifestUrl, e);
            return ImmutablePair.of(ManifestStatus.COULD_NOT_PARSE, null);
        } catch (Exception e) {
            LOGGER.warn("Could not read manifest.json from parcel repo: {}, message: {}", manifestUrl, e);
            return ImmutablePair.of(ManifestStatus.FAILED, null);
        }
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
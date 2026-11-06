package com.sequenceiq.maintenance.authorization;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaInternalV1Endpoint;

@Service
public class FreeIpaEnvironmentCrnLookupClient implements FreeIpaEnvironmentCrnLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaEnvironmentCrnLookupClient.class);

    private final FreeIpaInternalV1Endpoint freeIpaInternalV1Endpoint;

    @Inject
    public FreeIpaEnvironmentCrnLookupClient(FreeIpaInternalV1Endpoint freeIpaInternalV1Endpoint) {
        this.freeIpaInternalV1Endpoint = freeIpaInternalV1Endpoint;
    }

    @Override
    public Optional<String> lookupEnvironmentCrn(String freeIpaResourceCrn, String accountId) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> Optional.ofNullable(freeIpaInternalV1Endpoint.getEnvironmentCrnByResourceCrn(freeIpaResourceCrn))
                            .map(response -> response.getEnvironmentCrn()),
                    accountId);
        } catch (NotFoundException notFound) {
            return Optional.empty();
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to resolve environment CRN for FreeIPA resource {} in account {}", freeIpaResourceCrn, accountId, ex);
            return Optional.empty();
        }
    }
}

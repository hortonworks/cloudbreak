package com.sequenceiq.freeipa.service.freeipa.user.ums;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;

@Service
public class UmsUsersStateProviderDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateProviderDispatcher.class);

    @Inject
    private BulkUmsUsersStateProvider bulkUmsUsersStateProvider;

    public Map<String, UmsUsersState> getEnvToUmsUsersStateMap(
            String accountId, Collection<String> environmentCrns, Optional<String> requestIdOptional) {
        try {
            LOGGER.debug("Getting UMS state for environments {} with requestId {}", environmentCrns, requestIdOptional);
            return bulkUmsUsersStateProvider.get(accountId, environmentCrns, requestIdOptional);
        } catch (RuntimeException e) {
            throw new UmsOperationException(String.format("Error during UMS operation: '%s'", e.getLocalizedMessage()), e);
        }
    }
}
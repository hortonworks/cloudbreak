package com.sequenceiq.freeipa.service.freeipa.user.ums;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private DefaultUmsUsersStateProvider defaultUmsUsersStateProvider;

    @Inject
    private BulkUmsUsersStateProvider bulkUmsUsersStateProvider;

    public Map<String, UmsUsersState> getEnvToUmsUsersStateMap(
            String accountId, Collection<String> environmentCrns,
            Set<String> userCrns, Set<String> machineUserCrns, Optional<String> requestIdOptional) {
        try {
            LOGGER.debug("Getting UMS state for environments {} with requestId {}", environmentCrns, requestIdOptional);

            boolean fullSync = userCrns.isEmpty() && machineUserCrns.isEmpty();

            if (fullSync) {
                return dispatchBulk(accountId, environmentCrns, userCrns, machineUserCrns,
                        requestIdOptional, fullSync);
            } else {
                return dispatchDefault(accountId, environmentCrns, userCrns, machineUserCrns,
                        requestIdOptional, fullSync);
            }
        } catch (RuntimeException e) {
            throw new UmsOperationException(String.format("Error during UMS operation: '%s'", e.getLocalizedMessage()), e);
        }
    }

    private Map<String, UmsUsersState> dispatchBulk(
            String accountId, Collection<String> environmentCrns,
            Set<String> userCrns, Set<String> machineUserCrns, Optional<String> requestIdOptional,
            boolean fullSync) {
        try {
            return bulkUmsUsersStateProvider.get(accountId, environmentCrns, requestIdOptional);
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to retrieve UMS user sync state through bulk request. Falling back on default approach");
            return dispatchDefault(accountId, environmentCrns, userCrns, machineUserCrns,
                    requestIdOptional, fullSync);
        }
    }

    private Map<String, UmsUsersState> dispatchDefault(
            String accountId, Collection<String> environmentCrns,
            Set<String> userCrns, Set<String> machineUserCrns, Optional<String> requestIdOptional,
            boolean fullSync) {
        return defaultUmsUsersStateProvider.get(
                accountId,
                environmentCrns, userCrns, machineUserCrns,
                requestIdOptional, fullSync);
    }
}
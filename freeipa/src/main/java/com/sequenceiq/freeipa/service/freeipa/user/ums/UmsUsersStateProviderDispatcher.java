package com.sequenceiq.freeipa.service.freeipa.user.ums;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import com.sequenceiq.freeipa.service.freeipa.user.UserSyncRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;

import static com.google.common.base.Preconditions.checkArgument;

@Service
public class UmsUsersStateProviderDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateProviderDispatcher.class);

    @Inject
    private DefaultUmsUsersStateProvider defaultUmsUsersStateProvider;

    @Inject
    private BulkUmsUsersStateProvider bulkUmsUsersStateProvider;

    public Map<String, UmsUsersState> getEnvToUmsUsersStateMap(
            String accountId, String actorCrn, Collection<String> environmentCrns,
            UserSyncRequestFilter userSyncRequestFilter, Optional<String> requestIdOptional,
            BiConsumer<String, String> warnings) {
        try {
            LOGGER.debug("Getting UMS state for environments {} with requestId {}", environmentCrns, requestIdOptional);

            if (userSyncRequestFilter.isFullSync()) {
                return dispatchBulk(accountId, actorCrn, environmentCrns, userSyncRequestFilter,
                        requestIdOptional, warnings);
            } else {
                return dispatchDefault(accountId, actorCrn, environmentCrns, userSyncRequestFilter,
                        requestIdOptional, warnings);
            }
        } catch (RuntimeException e) {
            throw new UmsOperationException(String.format("Error during UMS operation: '%s'", e.getLocalizedMessage()), e);
        }
    }

    private Map<String, UmsUsersState> dispatchBulk(
            String accountId, String actorCrn, Collection<String> environmentCrns,
            UserSyncRequestFilter userSyncRequestFilter, Optional<String> requestIdOptional,
            BiConsumer<String, String> warnings) {
        checkArgument(userSyncRequestFilter.isFullSync(), "Bulk UMS state generation only available for full syncs");
        try {
            return bulkUmsUsersStateProvider.get(accountId, environmentCrns, requestIdOptional);
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to retrieve UMS user sync state through bulk request. Falling back on default approach");
            return dispatchDefault(accountId, actorCrn, environmentCrns, userSyncRequestFilter,
                    requestIdOptional, warnings);
        }
    }

    private Map<String, UmsUsersState> dispatchDefault(
            String accountId, String actorCrn, Collection<String> environmentCrns,
            UserSyncRequestFilter userSyncRequestFilter, Optional<String> requestIdOptional,
            BiConsumer<String, String> warnings) {
        return defaultUmsUsersStateProvider.get(
                accountId, actorCrn,
                environmentCrns, userSyncRequestFilter,
                requestIdOptional, warnings);
    }
}
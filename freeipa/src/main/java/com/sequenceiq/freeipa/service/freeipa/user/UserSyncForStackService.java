package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.ADD_SUDO_RULES;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.APPLY_DIFFERENCE_TO_IPA;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.CALCULATE_UMS_IPA_DIFFERENCE;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.RETRIEVE_FULL_IPA_STATE;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.RETRIEVE_PARTIAL_IPA_STATE;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.SYNC_CLOUD_IDENTITIES;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.USER_SYNC_DELETE;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.model.SyncStatusDetail;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;

@Service
public class UserSyncForStackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncForStackService.class);

    @Value("${freeipa.usersync.scale.large-group-size}")
    private int largeGroupLogThreshold;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private FreeIpaUsersStateProvider freeIpaUsersStateProvider;

    @Inject
    private CloudIdentitySyncService cloudIdentitySyncService;

    @Inject
    private UserSyncStateApplier stateApplier;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SudoRuleService sudoRuleService;

    @Inject
    private UserStateDifferenceCalculator userStateDifferenceCalculator;

    public SyncStatusDetail synchronizeStack(Stack stack, UmsUsersState umsUsersState, UserSyncOptions options) {
        MDCBuilder.buildMdcContext(stack);
        String environmentCrn = stack.getEnvironmentCrn();
        Multimap<String, String> warnings = ArrayListMultimap.create();
        logLargeGroupMembershipSizes(environmentCrn, umsUsersState);
        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            UsersStateDifference usersStateDifferenceBeforeSync = compareUmsAndFreeIpa(umsUsersState, options, freeIpaClient);
            stateApplier.applyDifference(umsUsersState, environmentCrn, warnings, usersStateDifferenceBeforeSync, options, freeIpaClient);

            retrySyncIfBatchCallHasWarnings(stack, umsUsersState, warnings, options, freeIpaClient, usersStateDifferenceBeforeSync);

            if (options.isFullSync()) {
                // TODO For now we only sync cloud ids during full sync. We should eventually allow more granular syncs (actor level and group level sync).
                if (entitlementService.cloudIdentityMappingEnabled(stack.getAccountId())) {
                    LOGGER.debug("Starting {} ...", SYNC_CLOUD_IDENTITIES);
                    cloudIdentitySyncService.syncCloudIdentities(stack, umsUsersState, warnings::put);
                    LOGGER.debug("Finished {}.", SYNC_CLOUD_IDENTITIES);
                }

                if (entitlementService.isEnvironmentPrivilegedUserEnabled(stack.getAccountId())) {
                    LOGGER.debug("Starting {} ...", ADD_SUDO_RULES);
                    try {
                        sudoRuleService.setupSudoRule(stack, freeIpaClient);
                    } catch (Exception e) {
                        warnings.put(stack.getEnvironmentCrn(), e.getMessage());
                        LOGGER.error("{} failed for environment '{}'.", ADD_SUDO_RULES, stack.getEnvironmentCrn(), e);
                    }
                    LOGGER.debug("Finished {}.", ADD_SUDO_RULES);
                }
            }

            return toSyncStatusDetail(environmentCrn, warnings);
        } catch (TimeoutException e) {
            LOGGER.warn("Timed out while synchronizing environment {}", environmentCrn, e);
            return SyncStatusDetail.fail(environmentCrn, "Timed out", warnings);
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize environment {}", environmentCrn, e);
            return SyncStatusDetail.fail(environmentCrn, e.getLocalizedMessage(), warnings);
        }
    }

    private void logLargeGroupMembershipSizes(String envCrn, UmsUsersState umsUsersState) {
        if (LOGGER.isDebugEnabled()) {
            List<Integer> largeGroupSizes = umsUsersState.getUsersState().getGroupMembership()
                    .asMap().values().stream()
                    .map(collection -> collection.size())
                    .filter(size -> size >= largeGroupLogThreshold)
                    .sorted()
                    .collect(Collectors.toList());
            if (!largeGroupSizes.isEmpty()) {
                LOGGER.debug("Environment {} has {} groups with size >= {}. {}",
                        envCrn,
                        largeGroupSizes.size(),
                        largeGroupLogThreshold,
                        largeGroupSizes);
            }
        }
    }

    public SyncStatusDetail synchronizeStackForDeleteUser(Stack stack, String deletedWorkloadUser) {
        MDCBuilder.buildMdcContext(stack);
        String environmentCrn = stack.getEnvironmentCrn();
        Multimap<String, String> warnings = ArrayListMultimap.create();
        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);

            LOGGER.debug("Starting {} for environment {} and deleted user {} ...", USER_SYNC_DELETE, environmentCrn, deletedWorkloadUser);

            LOGGER.debug("Starting {} ...", RETRIEVE_PARTIAL_IPA_STATE);
            UsersState ipaUserState = getIpaStateForUser(freeIpaClient, deletedWorkloadUser);
            LOGGER.debug("Finished {}, found {} users and {} groups.", RETRIEVE_PARTIAL_IPA_STATE, ipaUserState.getUsers().size(),
                    ipaUserState.getGroups().size());

            if (!ipaUserState.getUsers().isEmpty()) {
                ImmutableCollection<String> groupMembershipsToRemove = ipaUserState.getGroupMembership().get(deletedWorkloadUser);
                UsersStateDifference usersStateDifference = userStateDifferenceCalculator.forDeletedUser(deletedWorkloadUser, groupMembershipsToRemove);
                LOGGER.debug("Starting {} ...", APPLY_DIFFERENCE_TO_IPA);
                stateApplier.applyStateDifferenceToIpa(stack.getEnvironmentCrn(), freeIpaClient, usersStateDifference, warnings::put, false);
                LOGGER.debug("Finished {}.", APPLY_DIFFERENCE_TO_IPA);
            }

            LOGGER.debug("Finished {} for environment {} and deleted user {} ...", USER_SYNC_DELETE, environmentCrn, deletedWorkloadUser);
            return toSyncStatusDetail(environmentCrn, warnings);
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize environment {}", environmentCrn, e);
            return SyncStatusDetail.fail(environmentCrn, e.getLocalizedMessage(), warnings);
        }
    }

    private void retrySyncIfBatchCallHasWarnings(Stack stack, UmsUsersState umsUsersState, Multimap<String, String> warnings, UserSyncOptions options,
            FreeIpaClient freeIpaClient, UsersStateDifference usersStateDifferenceBeforeSync) throws FreeIpaClientException, TimeoutException {
        if (options.isFullSync() && !warnings.isEmpty() && options.isFmsToFreeIpaBatchCallEnabled()) {
            UsersStateDifference usersStateDifferenceAfterSync = compareUmsAndFreeIpa(umsUsersState, options, freeIpaClient);
            if (userStateDifferenceCalculator.usersStateDifferenceChanged(usersStateDifferenceBeforeSync, usersStateDifferenceAfterSync)) {
                Multimap<String, String> retryWarnings = ArrayListMultimap.create();
                try {
                    LOGGER.info(String.format("Sync was partially successful for %s, thus we are trying it once again", stack.getResourceCrn()));
                    stateApplier.applyDifference(umsUsersState, stack.getEnvironmentCrn(), retryWarnings, usersStateDifferenceAfterSync, options, freeIpaClient);
                    warnings.clear();
                } finally {
                    warnings.putAll(retryWarnings);
                }
            }
        }
    }

    private UsersState getIpaStateForUser(FreeIpaClient freeIpaClient, String workloadUserName) throws FreeIpaClientException {
        return freeIpaUsersStateProvider.getFilteredFreeIpaState(freeIpaClient, Set.of(workloadUserName));
    }

    private SyncStatusDetail toSyncStatusDetail(String environmentCrn, Multimap<String, String> warnings) {
        if (warnings.isEmpty()) {
            return SyncStatusDetail.succeed(environmentCrn);
        } else {
            return SyncStatusDetail.fail(environmentCrn, "Synchronization completed with warnings.", warnings);
        }
    }

    private UsersStateDifference compareUmsAndFreeIpa(UmsUsersState umsUsersState, UserSyncOptions options, FreeIpaClient freeIpaClient)
            throws FreeIpaClientException {
        UserSyncLogEvent logEvent = options.isFullSync() ? RETRIEVE_FULL_IPA_STATE : RETRIEVE_PARTIAL_IPA_STATE;
        LOGGER.debug("Starting {} ...", logEvent);
        UsersState ipaUsersState = getIpaUserState(freeIpaClient, umsUsersState, options.isFullSync());
        LOGGER.debug("Finished {}, found {} users and {} groups.", logEvent,
                ipaUsersState.getUsers().size(), ipaUsersState.getGroups().size());

        LOGGER.debug("Starting {} ...", CALCULATE_UMS_IPA_DIFFERENCE);
        UsersStateDifference usersStateDifference = userStateDifferenceCalculator.fromUmsAndIpaUsersStates(umsUsersState, ipaUsersState, options);
        LOGGER.debug("Finished {}.", CALCULATE_UMS_IPA_DIFFERENCE);

        return usersStateDifference;
    }

    private UsersState getIpaUserState(FreeIpaClient freeIpaClient, UmsUsersState umsUsersState, boolean fullSync)
            throws FreeIpaClientException {
        return fullSync ? freeIpaUsersStateProvider.getUsersState(freeIpaClient) :
                freeIpaUsersStateProvider.getFilteredFreeIpaState(
                        freeIpaClient, umsUsersState.getRequestedWorkloadUsernames());
    }
}

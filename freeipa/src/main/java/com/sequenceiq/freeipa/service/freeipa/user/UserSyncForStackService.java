package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.ADD_SUDO_RULES;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.APPLY_DIFFERENCE_TO_IPA;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.CALCULATE_UMS_IPA_DIFFERENCE;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.RETRIEVE_FULL_IPA_STATE;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.RETRIEVE_PARTIAL_IPA_STATE;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.SYNC_CLOUD_IDENTITIES;
import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncLogEvent.USER_SYNC_DELETE;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.model.SyncStatusDetail;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;

@Service
public class UserSyncForStackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncForStackService.class);

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

    @Inject
    private AuthDistributorService authDistributorService;

    public SyncStatusDetail synchronizeStack(StackUserSyncView stack, UmsUsersState umsUsersState, UserSyncOptions options, String operationId) {
        MDCBuilder.buildMdcContext(stack);
        String environmentCrn = stack.environmentCrn();
        Multimap<String, String> warnings = ArrayListMultimap.create();
        logLargeGroupMembershipSizes(environmentCrn, umsUsersState, options);
        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackId(stack.id());
            UsersStateDifference usersStateDifferenceBeforeSync = compareUmsAndFreeIpa(umsUsersState, options, freeIpaClient, warnings::put);
            stateApplier.applyDifference(umsUsersState, environmentCrn, warnings, usersStateDifferenceBeforeSync, options, freeIpaClient);

            retrySyncIfBatchCallHasWarnings(stack, umsUsersState, warnings, options, freeIpaClient, usersStateDifferenceBeforeSync);

            if (options.isFullSync()) {
                // TODO For now we only sync cloud ids during full sync. We should eventually allow more granular syncs (actor level and group level sync).
                if (entitlementService.cloudIdentityMappingEnabled(stack.accountId())) {
                    LOGGER.debug("Starting {} ...", SYNC_CLOUD_IDENTITIES);
                    cloudIdentitySyncService.syncCloudIdentities(stack, umsUsersState, warnings::put);
                    LOGGER.debug("Finished {}.", SYNC_CLOUD_IDENTITIES);
                }

                if (entitlementService.isEnvironmentPrivilegedUserEnabled(stack.accountId())) {
                    LOGGER.debug("Starting {} ...", ADD_SUDO_RULES);
                    try {
                        sudoRuleService.setupSudoRule(stack, freeIpaClient);
                    } catch (Exception e) {
                        warnings.put(stack.environmentCrn(), e.getMessage());
                        LOGGER.error("{} failed for environment '{}'.", ADD_SUDO_RULES, stack.environmentCrn(), e);
                    }
                    LOGGER.debug("Finished {}.", ADD_SUDO_RULES);
                }
            }

            SyncStatusDetail syncStatusDetail = toSyncStatusDetail(environmentCrn, warnings);
            LOGGER.debug("Stack sync status: {}, environmentCrn: {}, fullSync: {}", syncStatusDetail.getStatus(), environmentCrn, options.isFullSync());
            if (options.isFullSync() && SynchronizationStatus.COMPLETED.equals(syncStatusDetail.getStatus())) {
                authDistributorService.updateAuthViewForEnvironment(environmentCrn, umsUsersState, stack.accountId(), operationId);
            }
            return syncStatusDetail;
        } catch (TimeoutException e) {
            LOGGER.warn("Timed out while synchronizing environment {}", environmentCrn, e);
            return SyncStatusDetail.fail(environmentCrn, "Timed out", warnings);
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize environment {}", environmentCrn, e);
            return SyncStatusDetail.fail(environmentCrn, e.getLocalizedMessage(), warnings);
        }
    }

    private void logLargeGroupMembershipSizes(String envCrn, UmsUsersState umsUsersState, UserSyncOptions options) {
        int largeGroupThreshold = options.getLargeGroupThreshold();
        if (LOGGER.isDebugEnabled()) {
            Map<String, Collection<String>> groupMemberships = umsUsersState.getUsersState().getGroupMembership().asMap();
            Map<String, Integer> largeGroups = umsUsersState.getGroupsExceedingThreshold().stream()
                    .collect(Collectors.toMap(Function.identity(), groupName -> groupMemberships.get(groupName).size()));
            if (!largeGroups.isEmpty()) {
                LOGGER.debug("Environment {} has {} groups with size >= {}. {}",
                        envCrn,
                        largeGroups.size(),
                        largeGroupThreshold,
                        largeGroups);
            }
        }
    }

    public SyncStatusDetail synchronizeStackForDeleteUser(StackUserSyncView stack, String deletedWorkloadUser) {
        MDCBuilder.buildMdcContext(stack);
        String environmentCrn = stack.environmentCrn();
        Multimap<String, String> warnings = ArrayListMultimap.create();
        try {
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackId(stack.id());

            LOGGER.debug("Starting {} for environment {} and deleted user {} ...", USER_SYNC_DELETE, environmentCrn, deletedWorkloadUser);

            LOGGER.debug("Starting {} ...", RETRIEVE_PARTIAL_IPA_STATE);
            UsersState ipaUserState = getIpaStateForUser(freeIpaClient, deletedWorkloadUser);
            LOGGER.debug("Finished {}, found {} users and {} groups.", RETRIEVE_PARTIAL_IPA_STATE, ipaUserState.getUsers().size(),
                    ipaUserState.getGroups().size());

            if (!ipaUserState.getUsers().isEmpty()) {
                LOGGER.debug("Starting {} ...", APPLY_DIFFERENCE_TO_IPA);
                stateApplier.applyUserDeleteToIpa(stack.environmentCrn(), freeIpaClient, deletedWorkloadUser, warnings::put, false);
                LOGGER.debug("Finished {}.", APPLY_DIFFERENCE_TO_IPA);
            }

            LOGGER.debug("Finished {} for environment {} and deleted user {} ...", USER_SYNC_DELETE, environmentCrn, deletedWorkloadUser);
            return toSyncStatusDetail(environmentCrn, warnings);
        } catch (Exception e) {
            LOGGER.warn("Failed to synchronize environment {}", environmentCrn, e);
            return SyncStatusDetail.fail(environmentCrn, e.getLocalizedMessage(), warnings);
        }
    }

    private void retrySyncIfBatchCallHasWarnings(StackUserSyncView stack, UmsUsersState umsUsersState, Multimap<String, String> warnings,
            UserSyncOptions options, FreeIpaClient freeIpaClient, UsersStateDifference usersStateDifferenceBeforeSync)
            throws FreeIpaClientException, TimeoutException {
        if (options.isFullSync() && !warnings.isEmpty() && options.isFmsToFreeIpaBatchCallEnabled()) {
            UsersStateDifference usersStateDifferenceAfterSync = compareUmsAndFreeIpa(umsUsersState, options, freeIpaClient, warnings::put);
            if (userStateDifferenceCalculator.usersStateDifferenceChanged(usersStateDifferenceBeforeSync, usersStateDifferenceAfterSync)) {
                Multimap<String, String> retryWarnings = ArrayListMultimap.create();
                try {
                    LOGGER.info(String.format("Sync was partially successful for %s, thus we are trying it once again", stack.resourceCrn()));
                    stateApplier.applyDifference(umsUsersState, stack.environmentCrn(), retryWarnings, usersStateDifferenceAfterSync, options, freeIpaClient);
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

    private UsersStateDifference compareUmsAndFreeIpa(UmsUsersState umsUsersState, UserSyncOptions options, FreeIpaClient freeIpaClient,
            BiConsumer<String, String> warnings) throws FreeIpaClientException {
        UserSyncLogEvent logEvent = options.isFullSync() ? RETRIEVE_FULL_IPA_STATE : RETRIEVE_PARTIAL_IPA_STATE;
        LOGGER.debug("Starting {} ...", logEvent);
        UsersState ipaUsersState = getIpaUserState(freeIpaClient, umsUsersState, options);
        LOGGER.debug("Finished {}, found {} users and {} groups.", logEvent,
                ipaUsersState.getUsers().size(), ipaUsersState.getGroups().size());

        LOGGER.debug("Starting {} ...", CALCULATE_UMS_IPA_DIFFERENCE);
        UsersStateDifference usersStateDifference = userStateDifferenceCalculator.fromUmsAndIpaUsersStates(umsUsersState, ipaUsersState, options, warnings);
        LOGGER.debug("Finished {}.", CALCULATE_UMS_IPA_DIFFERENCE);

        return usersStateDifference;
    }

    private UsersState getIpaUserState(FreeIpaClient freeIpaClient, UmsUsersState umsUsersState, UserSyncOptions options)
            throws FreeIpaClientException {
        return options.isFullSync() ? freeIpaUsersStateProvider.getUsersState(freeIpaClient, options.isSplitFreeIPAUserRetrievalEnabled()) :
                freeIpaUsersStateProvider.getFilteredFreeIpaState(
                        freeIpaClient, umsUsersState.getRequestedWorkloadUsernames());
    }
}

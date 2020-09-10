package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.service.freeipa.user.model.Conversions;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CloudIdentity;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RightsCheckResult;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ServicePrincipalCloudIdentities;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadAdministrationGroup;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.Crn.ResourceType;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.freeipa.service.freeipa.user.model.EnvironmentAccessRights;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Service
public class UmsUsersStateProvider {
    @VisibleForTesting
    static final boolean INCLUDE_INTERNAL_MACHINE_USERS = true;

    @VisibleForTesting
    static final List<String> RIGHTS = ImmutableList.of(
            AuthorizationResourceAction.ACCESS_ENVIRONMENT.getRight(),
            AuthorizationResourceAction.ADMIN_FREEIPA.getRight());

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsUsersStateProvider.class);

    private static final FmsGroup USERSYNC_INTERNAL_GROUP =
            Conversions.nameToGroup(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP);

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private UmsCredentialProvider umsCredentialProvider;

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private EntitlementService entitlementService;

    public Map<String, UmsUsersState> getEnvToUmsUsersStateMap(
            String accountId, String actorCrn, Collection<String> environmentCrns,
            Set<String> userCrns, Set<String> machineUserCrns, Optional<String> requestIdOptional) {
        try {
            LOGGER.debug("Getting UMS state for environments {} with requestId {}", environmentCrns, requestIdOptional);

            boolean fullSync = userCrns.isEmpty() && machineUserCrns.isEmpty();

            Map<String, UmsUsersState.Builder> envUsersStateMap;
            if (fullSync && entitlementService.umsUserSyncModelGenerationEnabled(INTERNAL_ACTOR_CRN, accountId)) {
                envUsersStateMap = getUmsUsersStateMapBulk(accountId, environmentCrns, requestIdOptional);
            } else {
                envUsersStateMap = getUmsUsersStateMap(
                        accountId, actorCrn,
                        environmentCrns, userCrns, machineUserCrns,
                        requestIdOptional, fullSync);
            }

            return envUsersStateMap.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> e.getValue().build()));
        } catch (RuntimeException e) {
            throw new UmsOperationException(String.format("Error during UMS operation: '%s'", e.getLocalizedMessage()), e);
        }
    }

    @VisibleForTesting
    Map<String, UmsUsersState.Builder> getUmsUsersStateMapBulk(
            String accountId, Collection<String> environmentCrns, Optional<String> requestIdOptional) {

        List<String> environmentCrnList = List.copyOf(environmentCrns);
        GetUserSyncStateModelResponse userSyncStateModel = grpcUmsClient.getUserSyncStateModel(
                INTERNAL_ACTOR_CRN,
                accountId,
                generateRightsChecksForEnvironments(environmentCrnList),
                requestIdOptional);

        Map<String, FmsGroup> groups = convertGroupsToFmsGroups(userSyncStateModel.getGroupList());
        Map<WorkloadAdministrationGroup, FmsGroup> wags =
                convertWagsToFmsGroups(userSyncStateModel.getWorkloadAdministrationGroupList());
        List<String> requestedWorkloadUsernames = userSyncStateModel.getActorList().stream()
                .map(UserManagementProto.UserSyncActor::getActorDetails)
                .map(UserManagementProto.UserSyncActorDetails::getWorkloadUsername)
                .collect(Collectors.toList());

        Map<String, UmsUsersState.Builder> umsUsersStateBuilderMap = Maps.newHashMap();
        IntStream.range(0, environmentCrnList.size())
                .forEach(environmentIndex -> {
                    String environmentCrn = environmentCrnList.get(environmentIndex);
                    UmsUsersState.Builder umsUsersStateBuilder = UmsUsersState.newBuilder()
                            .setWorkloadAdministrationGroups(wags.values());
                    UsersState.Builder usersStateBuilder = UsersState.newBuilder();
                    addRequestedWorkloadUsernames(umsUsersStateBuilder, requestedWorkloadUsernames);
                    addGroupsToUsersStateBuilder(usersStateBuilder, groups.values());
                    Set<String> wagNamesForOtherEnvironments =
                            addWagsToUsersStateBuilder(usersStateBuilder, wags, environmentCrn);
                    addActorsToUmsUsersStateBuilder(
                            umsUsersStateBuilder,
                            usersStateBuilder,
                            environmentIndex,
                            userSyncStateModel,
                            groups,
                            wagNamesForOtherEnvironments);
                    addServicePrinciplesCloudIdentities(
                            accountId, environmentCrn, umsUsersStateBuilder, requestIdOptional);
                    umsUsersStateBuilder.setUsersState(usersStateBuilder.build());
                    umsUsersStateBuilderMap.put(environmentCrn, umsUsersStateBuilder);
                });
        return umsUsersStateBuilderMap;
    }

    @VisibleForTesting
    Map<String, UmsUsersState.Builder> getUmsUsersStateMap(
            String accountId, String actorCrn, Collection<String> environmentCrns,
            Set<String> userCrns, Set<String> machineUserCrns, Optional<String> requestIdOptional,
            boolean fullSync) {
        List<User> users = getUsers(actorCrn, accountId, requestIdOptional, fullSync, userCrns);
        List<MachineUser> machineUsers = getMachineUsers(actorCrn, accountId, requestIdOptional, fullSync, machineUserCrns);

        Map<String, FmsGroup> crnToFmsGroup = convertGroupsToFmsGroups(
                grpcUmsClient.listAllGroups(INTERNAL_ACTOR_CRN, accountId, requestIdOptional));
        Map<WorkloadAdministrationGroup, FmsGroup> wags = convertWagsToFmsGroups(
                grpcUmsClient.listWorkloadAdministrationGroups(INTERNAL_ACTOR_CRN, accountId, requestIdOptional));
        List<String> requestedWorkloadUsernames = Streams.concat(
                users.stream().map(User::getWorkloadUsername),
                machineUsers.stream().map(MachineUser::getWorkloadUsername))
                .collect(Collectors.toList());

        Map<String, UmsUsersState.Builder> umsUsersStateBuilderMap = new HashMap<>();
        environmentCrns.forEach(environmentCrn -> {
            UmsUsersState.Builder umsUsersStateBuilder = new UmsUsersState.Builder()
                    .setWorkloadAdministrationGroups(wags.values());
            UsersState.Builder usersStateBuilder = new UsersState.Builder();
            addRequestedWorkloadUsernames(umsUsersStateBuilder, requestedWorkloadUsernames);
            addGroupsToUsersStateBuilder(usersStateBuilder, crnToFmsGroup.values());
            Set<String> wagNamesForOtherEnvironments =
                    addWagsToUsersStateBuilder(usersStateBuilder, wags, environmentCrn);

            EnvironmentAccessChecker environmentAccessChecker = createEnvironmentAccessChecker(environmentCrn);
            TriConsumer<String, FmsUser, List<CloudIdentity>> actorHandler =
                    (memberCrn, fmsUser, cloudIdentityList) -> {
                        try {
                            handleActor(umsUsersStateBuilder,
                                    usersStateBuilder,
                                    environmentAccessChecker.hasAccess(memberCrn, requestIdOptional),
                                    crnToFmsGroup,
                                    wagNamesForOtherEnvironments,
                                    fmsUser,
                                    () -> grpcUmsClient.listGroupsForMember(
                                            INTERNAL_ACTOR_CRN, accountId, memberCrn, requestIdOptional),
                                    () -> grpcUmsClient.listWorkloadAdministrationGroupsForMember(
                                            INTERNAL_ACTOR_CRN, memberCrn, requestIdOptional),
                                    () -> umsCredentialProvider.getCredentials(memberCrn, requestIdOptional),
                                    cloudIdentityList);
                        } catch (StatusRuntimeException e) {
                            // NOT_FOUND errors indicate that a user/machineUser has been deleted after we have
                            // retrieved the list of users/machineUsers from the UMS. Interrupt calculation of group
                            // membership.
                            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                                LOGGER.warn("Member CRN {} not found in UMS. Member will not be added to the UMS Users State. {}",
                                        memberCrn, e.getLocalizedMessage());
                            } else {
                                throw e;
                            }
                        }
                    };
            users.forEach(u -> actorHandler.accept(u.getCrn(), Conversions.toFmsUser(u), u.getCloudIdentitiesList()));
            machineUsers.forEach(mu -> actorHandler.accept(mu.getCrn(), Conversions.toFmsUser(mu), mu.getCloudIdentitiesList()));

            addServicePrinciplesCloudIdentities(
                    accountId, environmentCrn, umsUsersStateBuilder, requestIdOptional);

            umsUsersStateBuilder.setUsersState(usersStateBuilder.build());
            umsUsersStateBuilderMap.put(environmentCrn, umsUsersStateBuilder);
        });
        return umsUsersStateBuilderMap;
    }

    private void addActorsToUmsUsersStateBuilder(
            UmsUsersState.Builder umsUsersStateBuilder,
            UsersState.Builder usersStateBuilder,
            int environmentIndex,
            GetUserSyncStateModelResponse userSyncStateModel,
            Map<String, FmsGroup> crnToFmsGroup,
            Set<String> wagNamesForOtherEnvironments) {

        // process actors - users and machine users are combined in the actor list
        userSyncStateModel.getActorList().stream().forEach(actor -> {
            RightsCheckResult rightsCheckResult = actor.getRightsCheckResult(environmentIndex);
            EnvironmentAccessRights environmentAccessRights = new EnvironmentAccessRights(
                    rightsCheckResult.getHasRight(0),
                    rightsCheckResult.getHasRight(1));

            handleActor(umsUsersStateBuilder,
                    usersStateBuilder,
                    environmentAccessRights,
                    crnToFmsGroup,
                    wagNamesForOtherEnvironments,
                    Conversions.toFmsUser(actor.getActorDetails()),
                    () -> actor.getGroupIndexList().stream()
                            .map(groupIndex ->
                                    userSyncStateModel.getGroupList().get(groupIndex).getCrn())
                            .collect(Collectors.toList()),
                    () -> actor.getWorkloadAdministrationGroupIndexList().stream()
                            .map(wagIndex ->
                                    userSyncStateModel.getWorkloadAdministrationGroupList()
                                            .get(wagIndex).getWorkloadAdministrationGroupName())
                            .collect(Collectors.toList()),
                    () -> Conversions.toWorkloadCredential(actor.getCredentials()),
                    actor.getActorDetails().getCloudIdentityList());
        });

    }

    private void addRequestedWorkloadUsernames(
            UmsUsersState.Builder umsUsersStateBuilder, List<String> requestedWorkloadUsernames) {
        umsUsersStateBuilder.addAllRequestedWorkloadUsernames(requestedWorkloadUsernames);
    }

    private void addGroupsToUsersStateBuilder(UsersState.Builder builder, Collection<FmsGroup> groups) {
        groups.forEach(builder::addGroup);
        // Add internal usersync group for each environment
        builder.addGroup(USERSYNC_INTERNAL_GROUP);
    }

    private Set<String> addWagsToUsersStateBuilder(
            UsersState.Builder builder,
            Map<WorkloadAdministrationGroup, FmsGroup> wags,
            String environmentCrn) {
        Set<String> wagNamesForOtherEnvironments = new HashSet<>();
        // Only add workload admin groups that belong to this environment.
        // At the same time, build a set of workload admin groups that are
        // associated with other environments so we can filter these out in
        // the per-user group listing in handleUser.
        wags.entrySet().forEach(wagEntry -> {
            WorkloadAdministrationGroup wag = wagEntry.getKey();
            String groupName = wag.getWorkloadAdministrationGroupName();
            if (wag.getResource().equalsIgnoreCase(environmentCrn)) {
                builder.addGroup(wagEntry.getValue());
            } else {
                Crn resourceCrn = getCrn(wag);
                if (resourceCrn != null && resourceCrn.getService() == Crn.Service.ENVIRONMENTS
                        && resourceCrn.getResourceType() == ResourceType.ENVIRONMENT) {
                    wagNamesForOtherEnvironments.add(groupName);
                }
            }
        });
        return wagNamesForOtherEnvironments;
    }

    private Map<String, FmsGroup> convertGroupsToFmsGroups(List<Group> groups) {
        return groups.stream().collect(Collectors.toMap(Group::getCrn, Conversions::umsGroupToGroup));
    }

    private Map<WorkloadAdministrationGroup, FmsGroup> convertWagsToFmsGroups(List<WorkloadAdministrationGroup> wags) {
        return wags.stream()
                .collect(Collectors.toMap(wag -> wag, wag -> Conversions.nameToGroup(wag.getWorkloadAdministrationGroupName())));
    }

    private List<Pair<String, List<String>>> generateRightsChecksForEnvironments(Collection<String> environmentCrns) {
        return environmentCrns.stream()
                .map(crn -> Pair.of(crn, RIGHTS))
                .collect(Collectors.toList());
    }

    private List<User> getUsers(String actorCrn, String accountId, Optional<String> requestIdOptional, boolean fullSync, Set<String> userCrns) {
        if (fullSync) {
            return grpcUmsClient.listAllUsers(actorCrn, accountId, requestIdOptional);
        } else if (!userCrns.isEmpty()) {
            return grpcUmsClient.listUsers(actorCrn, accountId, List.copyOf(userCrns), requestIdOptional);
        } else {
            return List.of();
        }
    }

    private List<MachineUser> getMachineUsers(String actorCrn, String accountId, Optional<String> requestIdOptional,
        boolean fullSync, Set<String> machineUserCrns) {
        if (fullSync) {
            return grpcUmsClient.listAllMachineUsers(actorCrn, accountId,
                    INCLUDE_INTERNAL_MACHINE_USERS, requestIdOptional);
        } else if (!machineUserCrns.isEmpty()) {
            return grpcUmsClient.listMachineUsers(actorCrn, accountId, List.copyOf(machineUserCrns),
                    INCLUDE_INTERNAL_MACHINE_USERS, requestIdOptional);
        } else {
            return List.of();
        }
    }

    @SuppressWarnings("ParameterNumber")
    private void handleActor(
            UmsUsersState.Builder umsUsersStateBuilder,
            UsersState.Builder usersStateBuilder,
            EnvironmentAccessRights environmentAccessRights,
            Map<String, FmsGroup> crnToFmsGroup,
            Set<String> wagNamesForOtherEnvironments,
            FmsUser fmsUser,
            Supplier<Collection<String>> groupCrnMembershipSupplier,
            Supplier<Collection<String>> wagMembershipSupplier,
            Supplier<WorkloadCredential> workloadCredentialSupplier,
            List<CloudIdentity> cloudIdentityList) {

        if (environmentAccessRights.hasEnvironmentAccessRight()) {
            String workloadUsername = fmsUser.getName();

            // Retrieve all information from UMS before modifying to the UmsUsersState or UsersState. This is so that
            // we don't partially modify the state if the member has been deleted after we started the sync
            Collection<String> groupCrnsForMember = groupCrnMembershipSupplier.get();
            Collection<String> workloadAdministrationGroupsForMember = wagMembershipSupplier.get();
            WorkloadCredential workloadCredential = workloadCredentialSupplier.get();

            groupCrnsForMember.forEach(gcrn -> {
                FmsGroup group = crnToFmsGroup.get(gcrn);
                // If the group is null, then there has been a group membership change after we started the sync
                // the group and group membership will be updated on the next sync
                if (group != null) {
                    usersStateBuilder.addMemberToGroup(group.getName(), workloadUsername);
                } else {
                    LOGGER.warn("{} is a member of unexpected group {}. Group must have been added after UMS state calculation started",
                            workloadUsername, gcrn);
                }
            });
            workloadAdministrationGroupsForMember.stream()
                    .filter(wagName -> !wagNamesForOtherEnvironments.contains(wagName))
                    .forEach(wagName -> {
                        usersStateBuilder.addGroup(Conversions.nameToGroup(wagName));
                        usersStateBuilder.addMemberToGroup(wagName, workloadUsername);
                    });

            addMemberToInternalTrackingGroup(usersStateBuilder, workloadUsername);
            if (environmentAccessRights.hasAdminFreeIpaRight()) {
                usersStateBuilder.addMemberToGroup(UserSyncConstants.ADMINS_GROUP, workloadUsername);
            }

            umsUsersStateBuilder.addWorkloadCredentials(workloadUsername, workloadCredential);
            umsUsersStateBuilder.addUserCloudIdentities(workloadUsername, cloudIdentityList);
            usersStateBuilder.addUser(fmsUser);
        }
    }

    private void addServicePrinciplesCloudIdentities(
            String accountId, String environmentCrn,
            UmsUsersState.Builder builder, Optional<String> requestIdOptional) {
        List<ServicePrincipalCloudIdentities> servicePrincipalCloudIdentities =
                grpcUmsClient.listServicePrincipalCloudIdentities(INTERNAL_ACTOR_CRN, accountId, environmentCrn, requestIdOptional);
        builder.addServicePrincipalCloudIdentities(servicePrincipalCloudIdentities);
    }

    private void addMemberToInternalTrackingGroup(UsersState.Builder usersStateBuilder, String username) {
        usersStateBuilder.addMemberToGroup(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP, username);
    }

    private EnvironmentAccessChecker createEnvironmentAccessChecker(String environmentCrn) {
        requireNonNull(environmentCrn, "environmentCrn is null");
        return new EnvironmentAccessChecker(grpcUmsClient, umsRightProvider, environmentCrn);
    }

    private Crn getCrn(WorkloadAdministrationGroup wag) {
        Crn resourceCrn = null;
        try {
            resourceCrn = Crn.fromString(wag.getResource());
        } catch (Exception e) {
            LOGGER.debug("Invalid resource is assigned to workload admin group: {}", e.getMessage());
        }
        return resourceCrn;
    }
}
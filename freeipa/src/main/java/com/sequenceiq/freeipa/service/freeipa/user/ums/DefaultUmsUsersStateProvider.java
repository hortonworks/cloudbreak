package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsUserConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Component
public class DefaultUmsUsersStateProvider extends BaseUmsUsersStateProvider {
    @VisibleForTesting
    static final boolean DONT_INCLUDE_INTERNAL_MACHINE_USERS = false;

    @VisibleForTesting
    static final boolean INCLUDE_WORKLOAD_MACHINE_USERS = true;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUmsUsersStateProvider.class);

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private EnvironmentAccessCheckerFactory environmentAccessCheckerFactory;

    @Inject
    private UmsCredentialProvider umsCredentialProvider;

    @Inject
    private FmsUserConverter fmsUserConverter;

    public Map<String, UmsUsersState> get(
            String accountId, Collection<String> environmentCrns,
            Set<String> userCrns, Set<String> machineUserCrns, UserSyncOptions options) {
        List<UserManagementProto.User> users = getUsers(accountId, options.isFullSync(), userCrns);
        List<UserManagementProto.MachineUser> machineUsers =
                getMachineUsers(accountId, options.isFullSync(), machineUserCrns);

        Map<String, FmsGroup> crnToFmsGroup = convertGroupsToFmsGroups(grpcUmsClient.listAllGroups(accountId));
        Map<UserManagementProto.WorkloadAdministrationGroup, FmsGroup> wags = convertWagsToFmsGroups(grpcUmsClient.listWorkloadAdministrationGroups(accountId));
        Map<UserManagementProto.WorkloadAdministrationGroup, FmsGroup> environmentWags = filterEnvironmentWags(wags);
        List<String> requestedWorkloadUsernames = Streams.concat(
                users.stream().map(UserManagementProto.User::getWorkloadUsername),
                machineUsers.stream().map(UserManagementProto.MachineUser::getWorkloadUsername))
                .collect(Collectors.toList());

        ImmutableSet<FmsGroup> workloadAdminGroups = ImmutableSet.copyOf(wags.values());
        LOGGER.debug("WAG size: {}", workloadAdminGroups.size());
        Map<String, UmsUsersState> umsUsersStateMap = new HashMap<>();
        environmentCrns.forEach(environmentCrn -> {
            UmsUsersState.Builder umsUsersStateBuilder = new UmsUsersState.Builder()
                    .setWorkloadAdministrationGroups(workloadAdminGroups);
            UsersState.Builder usersStateBuilder = new UsersState.Builder();

            addRequestedWorkloadUsernames(umsUsersStateBuilder, requestedWorkloadUsernames);
            addGroupsToUsersStateBuilder(usersStateBuilder, crnToFmsGroup.values());
            Set<String> wagNamesForOtherEnvironments =
                    addWagsToUsersStateBuilder(usersStateBuilder, environmentWags, environmentCrn);

            ActorHandler actorHandler = ActorHandler.newBuilder()
                    .withFmsGroupConverter(getFmsGroupConverter())
                    .withUmsUsersStateBuilder(umsUsersStateBuilder)
                    .withUsersStateBuilder(usersStateBuilder)
                    .withCrnToFmsGroup(crnToFmsGroup)
                    .withWagNamesForOtherEnvironments(wagNamesForOtherEnvironments)
                    .build();
            EnvironmentAccessChecker environmentAccessChecker = createEnvironmentAccessChecker(environmentCrn);
            addActorsToUmsUsersStateBuilder(accountId, environmentAccessChecker, users, machineUsers,
                    actorHandler);

            addServicePrincipalsCloudIdentities(
                    umsUsersStateBuilder,
                    grpcUmsClient.listServicePrincipalCloudIdentities(
                            accountId, environmentCrn));

            UsersState usersState = usersStateBuilder.build();
            umsUsersStateBuilder.setUsersState(usersState);

            setLargeGroups(umsUsersStateBuilder, usersState, options);

            umsUsersStateMap.put(environmentCrn, umsUsersStateBuilder.build());
        });
        return umsUsersStateMap;
    }

    private void addActorsToUmsUsersStateBuilder(
            String accountId, EnvironmentAccessChecker environmentAccessChecker,
            List<UserManagementProto.User> users, List<UserManagementProto.MachineUser> machineUsers,
            ActorHandler actorHandler) {
        Streams.concat(
                users.stream().map(user -> Triple.of(user.getCrn(),
                        fmsUserConverter.toFmsUser(user),
                        user.getCloudIdentitiesList())),
                machineUsers.stream().map(
                        machineUser -> Triple.of(machineUser.getCrn(),
                                fmsUserConverter.toFmsUser(machineUser),
                                machineUser.getCloudIdentitiesList())))
                .forEach(triple -> {
                    String memberCrn = triple.getLeft();
                    FmsUser fmsUser = triple.getMiddle();
                    List<UserManagementProto.CloudIdentity> cloudIdentityList = triple.getRight();

                    Supplier<Collection<String>> groupMembershipSupplier = () -> grpcUmsClient.listGroupsForMember(accountId, memberCrn);
                    Supplier<Collection<String>> wagMembershipSupplier = () -> grpcUmsClient.listWorkloadAdministrationGroupsForMember(memberCrn);
                    Supplier<WorkloadCredential> workloadCredentialSupplier = () -> umsCredentialProvider.getCredentials(memberCrn);

                    try {
                        actorHandler.handleActor(
                                environmentAccessChecker.hasAccess(memberCrn),
                                fmsUser,
                                memberCrn,
                                groupMembershipSupplier,
                                wagMembershipSupplier,
                                workloadCredentialSupplier,
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
                });
    }

    private List<UserManagementProto.User> getUsers(String accountId,
            boolean fullSync, Set<String> userCrns) {
        if (fullSync) {
            return grpcUmsClient.listAllUsers(accountId);
        } else if (!userCrns.isEmpty()) {
            return grpcUmsClient.listUsers(accountId, List.copyOf(userCrns));
        } else {
            return List.of();
        }
    }

    private List<UserManagementProto.MachineUser> getMachineUsers(
            String accountId, boolean fullSync, Set<String> machineUserCrns) {
        if (fullSync) {
            return grpcUmsClient.listAllMachineUsers(accountId, DONT_INCLUDE_INTERNAL_MACHINE_USERS, INCLUDE_WORKLOAD_MACHINE_USERS);
        } else if (!machineUserCrns.isEmpty()) {
            return grpcUmsClient.listMachineUsers(accountId, List.copyOf(machineUserCrns), DONT_INCLUDE_INTERNAL_MACHINE_USERS, INCLUDE_WORKLOAD_MACHINE_USERS);
        } else {
            return List.of();
        }
    }

    private EnvironmentAccessChecker createEnvironmentAccessChecker(String environmentCrn) {
        requireNonNull(environmentCrn, "environmentCrn is null");
        return environmentAccessCheckerFactory.create(environmentCrn);
    }
}

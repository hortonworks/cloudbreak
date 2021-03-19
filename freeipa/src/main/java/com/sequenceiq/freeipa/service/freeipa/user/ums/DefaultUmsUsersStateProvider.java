package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Streams;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsUserConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncRequestFilter;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Component
public class DefaultUmsUsersStateProvider extends BaseUmsUsersStateProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUmsUsersStateProvider.class);

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private EnvironmentAccessCheckerFactory environmentAccessCheckerFactory;

    @Inject
    private UmsCredentialProvider umsCredentialProvider;

    @Inject
    private FmsUserConverter fmsUserConverter;

    @Inject
    private UserRetriever userRetriever;

    @Inject
    private MachineUserRetriever machineUserRetriever;

    public Map<String, UmsUsersState> get(
            String accountId, String actorCrn,
            Collection<String> environmentCrns, UserSyncRequestFilter userSyncRequestFilter,
            Optional<String> requestIdOptional,
            BiConsumer<String, String> warnings) {
        boolean fullSync = userSyncRequestFilter.isFullSync();
        List<UserManagementProto.User> users = userRetriever.getUsers(actorCrn, accountId, requestIdOptional, fullSync,
                userSyncRequestFilter.getUserCrnFilter(), warnings);
        List<UserManagementProto.MachineUser> machineUsers =
                machineUserRetriever.getMachineUsers(actorCrn, accountId, requestIdOptional, fullSync,
                        userSyncRequestFilter.getMachineUserCrnFilter(), warnings);

        Map<String, FmsGroup> crnToFmsGroup = convertGroupsToFmsGroups(
                grpcUmsClient.listAllGroups(INTERNAL_ACTOR_CRN, accountId, requestIdOptional));
        Map<UserManagementProto.WorkloadAdministrationGroup, FmsGroup> wags = convertWagsToFmsGroups(
                grpcUmsClient.listWorkloadAdministrationGroups(INTERNAL_ACTOR_CRN, accountId, requestIdOptional));
        List<String> requestedWorkloadUsernames = Streams.concat(
                users.stream().map(UserManagementProto.User::getWorkloadUsername),
                machineUsers.stream().map(UserManagementProto.MachineUser::getWorkloadUsername))
                .collect(Collectors.toList());

        Map<String, UmsUsersState> umsUsersStateMap = new HashMap<>();
        environmentCrns.forEach(environmentCrn -> {
            UmsUsersState.Builder umsUsersStateBuilder = new UmsUsersState.Builder()
                    .setWorkloadAdministrationGroups(wags.values());
            UsersState.Builder usersStateBuilder = new UsersState.Builder();

            addRequestedWorkloadUsernames(umsUsersStateBuilder, requestedWorkloadUsernames);
            addGroupsToUsersStateBuilder(usersStateBuilder, crnToFmsGroup.values());
            Set<String> wagNamesForOtherEnvironments =
                    addWagsToUsersStateBuilder(usersStateBuilder, wags, environmentCrn);

            ActorHandler actorHandler = ActorHandler.newBuilder()
                    .withFmsGroupConverter(getFmsGroupConverter())
                    .withUmsUsersStateBuilder(umsUsersStateBuilder)
                    .withUsersStateBuilder(usersStateBuilder)
                    .withCrnToFmsGroup(crnToFmsGroup)
                    .withWagNamesForOtherEnvironments(wagNamesForOtherEnvironments)
                    .build();
            EnvironmentAccessChecker environmentAccessChecker = createEnvironmentAccessChecker(environmentCrn);
            addActorsToUmsUsersStateBuilder(accountId, environmentAccessChecker, users, machineUsers,
                    actorHandler, requestIdOptional);

            addServicePrincipalsCloudIdentities(
                    umsUsersStateBuilder,
                    grpcUmsClient.listServicePrincipalCloudIdentities(
                            INTERNAL_ACTOR_CRN, accountId, environmentCrn, requestIdOptional));

            umsUsersStateBuilder.setUsersState(usersStateBuilder.build());
            umsUsersStateMap.put(environmentCrn, umsUsersStateBuilder.build());
        });
        return umsUsersStateMap;
    }

    private void addActorsToUmsUsersStateBuilder(
            String accountId, EnvironmentAccessChecker environmentAccessChecker,
            List<UserManagementProto.User> users, List<UserManagementProto.MachineUser> machineUsers,
            ActorHandler actorHandler, Optional<String> requestIdOptional) {
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

                    Supplier<Collection<String>> groupMembershipSupplier = () ->
                            grpcUmsClient.listGroupsForMember(
                                    INTERNAL_ACTOR_CRN, accountId, memberCrn, requestIdOptional);
                    Supplier<Collection<String>> wagMembershipSupplier = () ->
                            grpcUmsClient.listWorkloadAdministrationGroupsForMember(
                                    INTERNAL_ACTOR_CRN, memberCrn, requestIdOptional);
                    Supplier<WorkloadCredential> workloadCredentialSupplier = () ->
                            umsCredentialProvider.getCredentials(memberCrn, requestIdOptional);

                    try {
                        actorHandler.handleActor(
                                environmentAccessChecker.hasAccess(memberCrn, requestIdOptional),
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

    private EnvironmentAccessChecker createEnvironmentAccessChecker(String environmentCrn) {
        requireNonNull(environmentCrn, "environmentCrn is null");
        return environmentAccessCheckerFactory.create(environmentCrn);
    }
}

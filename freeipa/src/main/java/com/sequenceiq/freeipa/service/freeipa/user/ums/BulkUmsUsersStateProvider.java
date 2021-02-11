package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsUserConverter;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.WorkloadCredentialConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.EnvironmentAccessRights;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

@Component
public class BulkUmsUsersStateProvider extends BaseUmsUsersStateProvider {
    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private UmsRightsChecksFactory umsRightsChecksFactory;

    @Inject
    private FmsUserConverter fmsUserConverter;

    @Inject
    private WorkloadCredentialConverter workloadCredentialConverter;

    public Map<String, UmsUsersState> get(
            String accountId, Collection<String> environmentCrns,
            Optional<String> requestIdOptional) {
        List<String> environmentCrnList = List.copyOf(environmentCrns);
        UserManagementProto.GetUserSyncStateModelResponse userSyncStateModel = grpcUmsClient.getUserSyncStateModel(
                INTERNAL_ACTOR_CRN,
                accountId,
                umsRightsChecksFactory.get(environmentCrnList),
                requestIdOptional);

        Map<String, FmsGroup> groups = convertGroupsToFmsGroups(userSyncStateModel.getGroupList());
        Map<UserManagementProto.WorkloadAdministrationGroup, FmsGroup> wags =
                convertWagsToFmsGroups(userSyncStateModel.getWorkloadAdministrationGroupList());
        List<String> requestedWorkloadUsernames = userSyncStateModel.getActorList().stream()
                .map(UserManagementProto.UserSyncActor::getActorDetails)
                .map(UserManagementProto.UserSyncActorDetails::getWorkloadUsername)
                .collect(Collectors.toList());

        Map<String, UmsUsersState> umsUsersStateMap = Maps.newHashMap();
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

                    ActorHandler actorHandler = ActorHandler.newBuilder()
                            .withFmsGroupConverter(getFmsGroupConverter())
                            .withUmsUsersStateBuilder(umsUsersStateBuilder)
                            .withUsersStateBuilder(usersStateBuilder)
                            .withCrnToFmsGroup(groups)
                            .withWagNamesForOtherEnvironments(wagNamesForOtherEnvironments)
                            .build();
                    addActorsToUmsUsersStateBuilder(
                            environmentIndex,
                            userSyncStateModel,
                            actorHandler);

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
            int environmentIndex,
            UserManagementProto.GetUserSyncStateModelResponse userSyncStateModel,
            ActorHandler actorHandler) {


        // process actors - users and machine users are combined in the actor list
        userSyncStateModel.getActorList().forEach(actor -> {
            UserManagementProto.RightsCheckResult rightsCheckResult = actor.getRightsCheckResult(environmentIndex);
            EnvironmentAccessRights environmentAccessRights = new EnvironmentAccessRights(
                    rightsCheckResult.getHasRight(0),
                    rightsCheckResult.getHasRight(1));
            Supplier<Collection<String>> groupMembershipSupplier = () ->
                    actor.getGroupIndexList().stream()
                            .map(groupIndex ->
                                    userSyncStateModel.getGroupList().get(groupIndex).getCrn())
                            .collect(Collectors.toList());
            Supplier<Collection<String>> wagMembershipSupplier = () ->
                    actor.getWorkloadAdministrationGroupIndexList().stream()
                            .map(wagIndex ->
                                    userSyncStateModel.getWorkloadAdministrationGroupList()
                                            .get(wagIndex).getWorkloadAdministrationGroupName())
                            .collect(Collectors.toList());
            Supplier<WorkloadCredential> workloadCredentialSupplier = () ->
                    workloadCredentialConverter.toWorkloadCredential(actor.getCredentials());

            actorHandler.handleActor(
                    environmentAccessRights,
                    fmsUserConverter.toFmsUser(actor.getActorDetails()),
                    actor.getActorDetails().getCrn(),
                    groupMembershipSupplier,
                    wagMembershipSupplier,
                    workloadCredentialSupplier,
                    actor.getActorDetails().getCloudIdentityList());
        });
    }

}

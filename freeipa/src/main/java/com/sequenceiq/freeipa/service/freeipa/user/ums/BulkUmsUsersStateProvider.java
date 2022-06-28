package com.sequenceiq.freeipa.service.freeipa.user.ums;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import com.sequenceiq.freeipa.service.freeipa.user.model.UserSyncOptions;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsUserConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.EnvironmentAccessRights;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@Component
public class BulkUmsUsersStateProvider extends BaseUmsUsersStateProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(BulkUmsUsersStateProvider.class);

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private UmsRightsChecksFactory umsRightsChecksFactory;

    @Inject
    private FmsUserConverter fmsUserConverter;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private UmsCredentialProvider umsCredentialProvider;

    public Map<String, UmsUsersState> get(
            String accountId, Collection<String> environmentCrns,
            Optional<String> requestIdOptional,
            UserSyncOptions options) {
        List<String> environmentCrnList = List.copyOf(environmentCrns);
        UserManagementProto.GetUserSyncStateModelResponse userSyncStateModel = grpcUmsClient.getUserSyncStateModel(
                accountId,
                umsRightsChecksFactory.get(environmentCrnList),
                true,
                requestIdOptional,
                regionAwareInternalCrnGeneratorFactory);

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
                            actorHandler,
                            requestIdOptional);

                    addServicePrincipalsCloudIdentities(
                            umsUsersStateBuilder,
                            grpcUmsClient.listServicePrincipalCloudIdentities(
                                    accountId, environmentCrn, requestIdOptional));

                    UsersState usersState = usersStateBuilder.build();
                    umsUsersStateBuilder.setUsersState(usersState);

                    setLargeGroups(umsUsersStateBuilder, usersState, options);

                    umsUsersStateMap.put(environmentCrn, umsUsersStateBuilder.build());
                });
        return umsUsersStateMap;
    }

    private void addActorsToUmsUsersStateBuilder(
            int environmentIndex,
            UserManagementProto.GetUserSyncStateModelResponse userSyncStateModel,
            ActorHandler actorHandler,
            Optional<String> requestIdOptional) {


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
                    umsCredentialProvider.getCredentials(actor.getActorDetails().getCrn(), requestIdOptional);

            try {
                actorHandler.handleActor(
                        environmentAccessRights,
                        fmsUserConverter.toFmsUser(actor.getActorDetails()),
                        actor.getActorDetails().getCrn(),
                        groupMembershipSupplier,
                        wagMembershipSupplier,
                        workloadCredentialSupplier,
                        actor.getActorDetails().getCloudIdentityList());
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                    LOGGER.warn("Member CRN {} not found in UMS. NOT_FOUND errors indicate that a user/machineUser " +
                                    "has been deleted after we have retrieved the list of users/machineUsers from " +
                                    "the UMS. Member will not be added to the UMS Users State. {}",
                            actor.getActorDetails().getCrn(), e.getLocalizedMessage());
                } else {
                    throw e;
                }
            }
        });
    }

}

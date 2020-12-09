package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.FmsUserConverter;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.WorkloadCredentialConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

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

        Map<UserManagementProto.WorkloadAdministrationGroup, FmsGroup> wags =
                convertWagsToFmsGroups(userSyncStateModel.getWorkloadAdministrationGroupList());

        Map<String, UmsUsersState> umsUsersStateMap = Maps.newHashMap();
        IntStream.range(0, environmentCrnList.size())
                .forEach(environmentIndex -> {
                    String environmentCrn = environmentCrnList.get(environmentIndex);
                    UmsUsersState.Builder umsUsersStateBuilder = UmsUsersState.newBuilder()
                            .setWorkloadAdministrationGroups(wags.values());
                    UsersState.Builder usersStateBuilder = UsersState.newBuilder();

                    List<UserManagementProto.WorkloadAdministrationGroup> orderedRelatedWags =
                            getRelatedWagsOrderedByRightCheck(wags, environmentCrn, usersStateBuilder);
                    List<String> resourceAssigneesCrns = getResourceAssignees(environmentCrn);
                    List<UserManagementProto.UserSyncActor> relatedActors = getRelatedActors(userSyncStateModel, resourceAssigneesCrns);
                    List<UserManagementProto.Group> relatedGroups = getRelatedGroups(userSyncStateModel, resourceAssigneesCrns);

                    Map<String, FmsGroup> groups = convertGroupsToFmsGroups(relatedGroups);
                    addGroupsToUsersStateBuilder(usersStateBuilder, groups.values());

                    ActorHandler actorHandler = ActorHandler.newBuilder()
                            .withFmsGroupConverter(getFmsGroupConverter())
                            .withUmsUsersStateBuilder(umsUsersStateBuilder)
                            .withUsersStateBuilder(usersStateBuilder)
                            .withCrnToFmsGroup(groups)
                            .build();
                    addActorsToUmsUsersStateBuilder(
                            environmentIndex,
                            relatedActors,
                            userSyncStateModel.getGroupList(),
                            relatedGroups,
                            orderedRelatedWags,
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

    private List<UserManagementProto.Group> getRelatedGroups(UserManagementProto.GetUserSyncStateModelResponse userSyncStateModel, List<String> resourceAssigneesCrns) {
        return userSyncStateModel.getGroupList().stream()
                .filter(group -> resourceAssigneesCrns.contains(group.getCrn()))
                .collect(Collectors.toList());
    }

    private List<UserManagementProto.UserSyncActor> getRelatedActors(UserManagementProto.GetUserSyncStateModelResponse userSyncStateModel, List<String> resourceAssigneesCrns) {
        return userSyncStateModel.getActorList().stream()
                .filter(userSyncActor -> resourceAssigneesCrns.contains(userSyncActor.getActorDetails().getCrn()) ||
                        userSyncActor.getGroupIndexList().stream()
                                .anyMatch(groupIndex -> resourceAssigneesCrns.contains(userSyncStateModel.getGroup(groupIndex).getCrn())))
                .filter(userSyncActor -> userSyncActor.getRightsCheckResultList().stream()
                        .anyMatch(rightsCheckResult -> rightsCheckResult.getHasRightList().stream().anyMatch(Boolean::booleanValue)))
                .collect(Collectors.toList());
    }

    private List<String> getResourceAssignees(String environmentCrn) {
        return grpcUmsClient.listAssigneesOfResource(INTERNAL_ACTOR_CRN, null, environmentCrn, MDCUtils.getRequestId())
                .stream()
                .map(UserManagementProto.ResourceAssignee::getAssigneeCrn)
                .collect(Collectors.toList());
    }

    private List<UserManagementProto.WorkloadAdministrationGroup> getRelatedWagsOrderedByRightCheck(Map<UserManagementProto.WorkloadAdministrationGroup, FmsGroup> wags, String environmentCrn, UsersState.Builder usersStateBuilder) {
        List<UserManagementProto.WorkloadAdministrationGroup> orderedRelatedWags = Lists.newArrayList();
        List<UserManagementProto.WorkloadAdministrationGroup> relatedWags =
                addWagsToUsersStateBuilder(usersStateBuilder, wags, environmentCrn);
        UserSyncConstants.RIGHTS.stream().forEach(right -> {
            Optional<UserManagementProto.WorkloadAdministrationGroup> wagByRight = relatedWags.stream().filter(wag ->
                    StringUtils.equals(wag.getRightName(), right)).findFirst();
            if (wagByRight.isPresent()) {
                orderedRelatedWags.add(wagByRight.get());
            }
        });
        return orderedRelatedWags;
    }

    private void addActorsToUmsUsersStateBuilder(
            int environmentIndex,
            List<UserManagementProto.UserSyncActor> relatedActors,
            List<UserManagementProto.Group> allGroups,
            List<UserManagementProto.Group> relatedGroups,
            List<UserManagementProto.WorkloadAdministrationGroup> orderedRelatedWags,
            ActorHandler actorHandler) {

        // process actors - users and machine users are combined in the actor list
        relatedActors.forEach(actor -> {
            UserManagementProto.RightsCheckResult rightsCheckResult = actor.getRightsCheckResult(environmentIndex);
            Supplier<Collection<String>> groupMembershipSupplier = () -> getGroupNamesByActor(allGroups, relatedGroups, actor);
            Supplier<Collection<String>> wagMembershipSupplier = () -> getVirtualGroupNamesByActor(orderedRelatedWags, rightsCheckResult);
            Supplier<WorkloadCredential> workloadCredentialSupplier = () ->
                    workloadCredentialConverter.toWorkloadCredential(actor.getCredentials());

            actorHandler.handleActor(
                    rightsCheckResult,
                    fmsUserConverter.toFmsUser(actor.getActorDetails()),
                    groupMembershipSupplier,
                    wagMembershipSupplier,
                    workloadCredentialSupplier,
                    actor.getActorDetails().getCloudIdentityList());
        });
    }

    private List<String> getVirtualGroupNamesByActor(List<UserManagementProto.WorkloadAdministrationGroup> orderedRelatedWags,
            UserManagementProto.RightsCheckResult rightsCheckResult) {
        Map<UserManagementProto.WorkloadAdministrationGroup, Boolean> result = Maps.newHashMap();
        orderedRelatedWags.stream().forEach(wag -> {
            int wagIndex = orderedRelatedWags.indexOf(wag);
            result.put(wag, rightsCheckResult.getHasRight(wagIndex));
        });
        return result.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(entry -> entry.getKey().getWorkloadAdministrationGroupName())
                .collect(Collectors.toList());
    }

    private List<String> getGroupNamesByActor(List<UserManagementProto.Group> allGroups, List<UserManagementProto.Group> relatedGroups, UserManagementProto.UserSyncActor actor) {
        return actor.getGroupIndexList().stream()
                .map(groupIndex -> allGroups.get(groupIndex).getCrn())
                .filter(groupCrn -> relatedGroups.stream().map(UserManagementProto.Group::getCrn)
                        .collect(Collectors.toList()).contains(groupCrn))
                .collect(Collectors.toList());
    }

}

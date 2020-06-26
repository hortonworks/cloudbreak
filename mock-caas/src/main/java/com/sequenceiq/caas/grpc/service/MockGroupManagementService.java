package com.sequenceiq.caas.grpc.service;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetWorkloadAdministrationGroupNameResponse.Builder;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SetWorkloadAdministrationGroupNameResponse;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.Crn.ResourceType;
import com.sequenceiq.cloudbreak.auth.altus.UmsRight;

import io.grpc.stub.StreamObserver;

@Service
class MockGroupManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockGroupManagementService.class);

    private static final String CM_ADMIN_RIGHT = "environments/adminClouderaManager";

    private static final int NUM_USER_GROUPS = 5;

    @Inject
    private MockCrnService mockCrnService;

    private final Map<String, Map<String, Group>> accountWorkloadGroups = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Group>> accountUserGroups = new ConcurrentHashMap<>();

    void listGroups(ListGroupsRequest request, StreamObserver<ListGroupsResponse> responseObserver) {
        ListGroupsResponse.Builder groupsBuilder = ListGroupsResponse.newBuilder();
        if (request.getGroupNameOrCrnCount() == 0) {
            if (isNotEmpty(request.getAccountId())) {
                getOrCreateUserGroups(request.getAccountId())
                        .forEach(groupsBuilder::addGroup);
            }
        } else {
            request.getGroupNameOrCrnList().stream()
                    .map(this::getOrCreateUserGroup)
                    .forEach(groupsBuilder::addGroup);
        }
        responseObserver.onNext(groupsBuilder.build());
        responseObserver.onCompleted();
    }

    List<Group> getOrCreateWorkloadGroups(String accountId) {
        accountWorkloadGroups.computeIfAbsent(accountId, this::createWorkloadGroups);
        List<Group> groups = new ArrayList<>(accountWorkloadGroups.get(accountId).values());
        groups.sort(Comparator.comparing(Group::getGroupName));
        return groups;
    }

    List<Group> getOrCreateUserGroups(String accountId) {
        accountUserGroups.computeIfAbsent(accountId, this::createUserGroups);
        List<Group> groups = new ArrayList<>(accountUserGroups.get(accountId).values());
        groups.sort(Comparator.comparing(Group::getGroupName));
        return groups;
    }

    void getWorkloadAdministrationGroupName(GetWorkloadAdministrationGroupNameRequest request,
            StreamObserver<GetWorkloadAdministrationGroupNameResponse> responseObserver) {
        mockCrnService.ensureInternalActor();
        Builder respBuilder =
                GetWorkloadAdministrationGroupNameResponse.getDefaultInstance().toBuilder();
        respBuilder.setWorkloadAdministrationGroupName(generateWorkloadGroupName(request.getRightName()));
        responseObserver.onNext(respBuilder.build());
        responseObserver.onCompleted();
    }

    void setWorkloadAdministrationGroupName(SetWorkloadAdministrationGroupNameRequest request,
            StreamObserver<SetWorkloadAdministrationGroupNameResponse> responseObserver) {
        mockCrnService.ensureInternalActor();
        SetWorkloadAdministrationGroupNameResponse.Builder respBuilder =
                SetWorkloadAdministrationGroupNameResponse.getDefaultInstance().toBuilder();
        respBuilder.setWorkloadAdministrationGroupName(generateWorkloadGroupName(request.getRightName()));
        responseObserver.onNext(respBuilder.build());
        responseObserver.onCompleted();
    }

    void deleteWorkloadAdministrationGroupName(DeleteWorkloadAdministrationGroupNameRequest request,
            StreamObserver<DeleteWorkloadAdministrationGroupNameResponse> responseObserver) {
        mockCrnService.ensureInternalActor();
        DeleteWorkloadAdministrationGroupNameResponse.Builder respBuilder =
                DeleteWorkloadAdministrationGroupNameResponse.getDefaultInstance().toBuilder();
        responseObserver.onNext(respBuilder.build());
        responseObserver.onCompleted();
    }

    String generateWorkloadGroupName(String umsRight) {
        String groupNamePostfix = umsRight.replaceAll("/", "_").toLowerCase();
        return "_c_" + groupNamePostfix;
    }

    Group createGroup(String accountId, String groupName) {
        String groupId = UUID.randomUUID().toString();
        String groupCrn = mockCrnService.createCrn(accountId, Crn.Service.IAM, ResourceType.GROUP, groupId).toString();
        return Group.newBuilder()
                .setGroupId(groupId)
                .setCrn(groupCrn)
                .setGroupName(groupName)
                .build();
    }

    private Map<String, Group> createWorkloadGroups(String accountId) {
        Map<String, Group> groups = new HashMap<>();
        for (UmsRight right : UmsRight.values()) {
            Group group = createGroup(accountId, generateWorkloadGroupName(right.getRight()));
            groups.put(group.getCrn(), group);
        }
        LOGGER.info("workload groups for user: {}", groups);
        return groups;
    }

    private Map<String, Group> createUserGroups(String accountId) {
        Map<String, Group> groups = new HashMap<>();
        for (int i = 0; i < NUM_USER_GROUPS; i++) {
            Group group = createGroup(accountId, "fakemockgroup" + i);
            groups.put(group.getCrn(), group);
        }
        LOGGER.info("user groups for user: {}", groups);
        return groups;
    }

    private Group getOrCreateUserGroup(String groupCrn) {
        String[] splittedCrn = groupCrn.split(":");
        String accountId = splittedCrn[4];

        accountUserGroups.computeIfAbsent(accountId, this::createUserGroups);
        Map<String, Group> groups = accountUserGroups.get(accountId);

        groups.computeIfAbsent(groupCrn, this::createGroupFromCrn);
        return groups.get(groupCrn);
    }

    private Group createGroupFromCrn(String groupCrn) {
        String[] splittedCrn = groupCrn.split(":");
        String groupId = splittedCrn[6];
        return Group.newBuilder()
                .setGroupId(groupId)
                .setCrn(groupCrn)
                .setGroupName(groupId)
                .build();
    }

}

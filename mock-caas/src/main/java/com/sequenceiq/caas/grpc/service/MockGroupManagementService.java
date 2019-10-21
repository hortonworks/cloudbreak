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

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

import io.grpc.stub.StreamObserver;

@Service
class MockGroupManagementService {

    private static final String ENVIRONMENT_ADMIN_RIGHT = "environments/write";

    @Inject
    private MockCrnService mockCrnService;

    private final Map<String, Map<String, UserManagementProto.Group>> accountGroups = new ConcurrentHashMap<>();

    void listGroups(UserManagementProto.ListGroupsRequest request, StreamObserver<UserManagementProto.ListGroupsResponse> responseObserver) {

        UserManagementProto.ListGroupsResponse.Builder groupsBuilder = UserManagementProto.ListGroupsResponse.newBuilder();
        if (request.getGroupNameOrCrnCount() == 0) {
            if (isNotEmpty(request.getAccountId())) {
                getOrCreateGroups(request.getAccountId()).stream()
                        .forEach(groupsBuilder::addGroup);
            }
            responseObserver.onNext(groupsBuilder.build());
        } else {
            request.getGroupNameOrCrnList().stream()
                    .map(this::getOrCreateGroup)
                    .forEach(groupsBuilder::addGroup);
            responseObserver.onNext(groupsBuilder.build());
        }
        responseObserver.onCompleted();
    }

    List<UserManagementProto.Group> getOrCreateGroups(String accountId) {
        accountGroups.computeIfAbsent(accountId, this::createVirtualGroups);
        List<UserManagementProto.Group> groups = new ArrayList<>(accountGroups.get(accountId).values());
        groups.sort(Comparator.comparing(UserManagementProto.Group::getGroupName));
        return groups;
    }

    void getWorkloadAdministrationGroupName(UserManagementProto.GetWorkloadAdministrationGroupNameRequest request,
            StreamObserver<UserManagementProto.GetWorkloadAdministrationGroupNameResponse> responseObserver) {
        UserManagementProto.GetWorkloadAdministrationGroupNameResponse.Builder respBuilder =
                UserManagementProto.GetWorkloadAdministrationGroupNameResponse.getDefaultInstance().toBuilder();
        respBuilder.setWorkloadAdministrationGroupName(generateVirtualGroupName(request.getRightName()));
        responseObserver.onNext(respBuilder.build());
        responseObserver.onCompleted();
    }

    void setWorkloadAdministrationGroupName(UserManagementProto.SetWorkloadAdministrationGroupNameRequest request,
            StreamObserver<UserManagementProto.SetWorkloadAdministrationGroupNameResponse> responseObserver) {
        UserManagementProto.SetWorkloadAdministrationGroupNameResponse.Builder respBuilder =
                UserManagementProto.SetWorkloadAdministrationGroupNameResponse.getDefaultInstance().toBuilder();
        respBuilder.setWorkloadAdministrationGroupName(generateVirtualGroupName(request.getRightName()));
        responseObserver.onNext(respBuilder.build());
        responseObserver.onCompleted();
    }

    void deleteWorkloadAdministrationGroupName(UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest request,
            StreamObserver<UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse> responseObserver) {
        UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse.Builder respBuilder =
                UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse.getDefaultInstance().toBuilder();
        responseObserver.onNext(respBuilder.build());
        responseObserver.onCompleted();
    }

    String generateVirtualGroupName(String umsRight) {
        String groupNamePostfix = umsRight.replaceAll("/", "_").toLowerCase();
        return "_c_" + groupNamePostfix;
    }

    private UserManagementProto.Group getOrCreateGroup(String groupCrn) {
        String[] splittedCrn = groupCrn.split(":");
        String accountId = splittedCrn[4];

        accountGroups.computeIfAbsent(accountId, this::createVirtualGroups);
        Map<String, UserManagementProto.Group> groups = accountGroups.get(accountId);

        groups.computeIfAbsent(groupCrn, this::createGroupFromCrn);
        return groups.get(groupCrn);
    }

    private Map<String, UserManagementProto.Group> createVirtualGroups(String accountId) {
        Map<String, UserManagementProto.Group> groups = new HashMap<>();
        UserManagementProto.Group group = createGroup(accountId, generateVirtualGroupName(ENVIRONMENT_ADMIN_RIGHT));
        groups.put(group.getCrn(), group);
        return groups;
    }

    private UserManagementProto.Group createGroup(String accountId, String groupName) {
        String groupId = UUID.randomUUID().toString();
        String groupCrn = mockCrnService.createCrn(accountId, Crn.Service.IAM, Crn.ResourceType.GROUP, groupId).toString();
        return UserManagementProto.Group.newBuilder()
                .setGroupId(groupId)
                .setCrn(groupCrn)
                .setGroupName(groupName)
                .build();
    }

    private UserManagementProto.Group createGroupFromCrn(String groupCrn) {
        String[] splittedCrn = groupCrn.split(":");
        String groupId = splittedCrn[6];
        return UserManagementProto.Group.newBuilder()
                .setGroupId(groupId)
                .setCrn(groupCrn)
                .setGroupName(groupId)
                .build();
    }
}

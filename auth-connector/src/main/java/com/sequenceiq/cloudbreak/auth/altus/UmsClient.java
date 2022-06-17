package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sequenceiq.cloudbreak.auth.altus.service.CrnChecker.warnIfAccountIdIsInternal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.NullableScalarTypeProto;
import com.cloudera.thunderhead.service.common.paging.PagingProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc;
import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc.UserManagementBlockingStub;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Actor;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AddMemberToGroupResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateGroupResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.DeleteGroupResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupMembersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsForMemberResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListGroupsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RemoveMemberFromGroupResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RightsCheck;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadAdministrationGroup;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsAuthenticationException;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.altus.CallingServiceNameInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentracing.Tracer;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class UmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsClient.class);

    private final ManagedChannel channel;

    private final UmsClientConfig umsClientConfig;

    private final Tracer tracer;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    /**
     * Constructor.
     *
     * @param channel the managed channel.
     * @param tracer  tracer
     */
    UmsClient(ManagedChannel channel, UmsClientConfig umsClientConfig, Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.channel = checkNotNull(channel, "channel should not be null.");
        this.umsClientConfig = checkNotNull(umsClientConfig, "umsClientConfig should not be null.");
        this.tracer = tracer;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    /**
     * Create new user group if it does not exist.
     *
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @param groupName the newly created group name
     * @return the new or existing user group.
     */
    public Group createGroup(String requestId, String accountId, String groupName) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(groupName, "groupName should not be null.");
        validateAccountIdWithWarning(accountId);

        try {
            CreateGroupResponse createGroupResponse = newStub(requestId).createGroup(
                    CreateGroupRequest.newBuilder()
                            .setAccountId(accountId)
                            .setGroupName(groupName)
                            .build()
            );
            LOGGER.info("New user group has been created: \nId: {} \nCrn: {} \nName: {}.", createGroupResponse.getGroup().getGroupId(),
                    createGroupResponse.getGroup().getCrn(), createGroupResponse.getGroup().getGroupName());
            return createGroupResponse.getGroup();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(io.grpc.Status.ALREADY_EXISTS.getCode())) {
                Group existingGroup = listGroups(requestId, accountId, List.of(groupName))
                        .stream()
                        .filter(foundGroup -> foundGroup.getGroupName().equals(groupName))
                        .findAny()
                        .orElse(null);
                LOGGER.info("User group already exists: \nId: {} \nCrn: {} \nName: {}.", existingGroup.getGroupId(), existingGroup.getCrn(),
                        existingGroup.getGroupName());
                return existingGroup;
            } else {
                throw e;
            }
        }
    }

    /**
     * Delete user group if it exist.
     *
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @param groupName the newly created group name
     */
    public void deleteGroup(String requestId, String accountId, String groupName) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(groupName, "groupName should not be null.");
        validateAccountIdWithWarning(accountId);

        try {
            DeleteGroupResponse deleteGroupResponse = newStub(requestId).deleteGroup(
                    DeleteGroupRequest.newBuilder()
                            .setAccountId(accountId)
                            .setGroupNameOrCrn(groupName)
                            .build()
            );
            LOGGER.info("User group has been deleted: \nName{} \nResponse: {}.", groupName, deleteGroupResponse.getAllFields());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.NOT_FOUND.getCode())) {
                LOGGER.info("User group '{}' not found or has already been deleted.", groupName);
            } else {
                throw e;
            }
        }
    }

    /**
     * Add member to the selected user group if it exist.
     *
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @param groupName the group where user is going to be assigned
     * @param memberCrn member (e.g., user) CRN
     */
    public void addMemberToGroup(String requestId, String accountId, String groupName, String memberCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(groupName, "groupName should not be null.");
        checkNotNull(memberCrn, "memberCrn should not be null.");
        validateAccountIdWithWarning(accountId);

        Crn crn = Crn.safeFromString(memberCrn);
        Actor.Builder actor = Actor.newBuilder().setAccountId(accountId);
        switch (crn.getResourceType()) {
            case USER:
                actor.setUserIdOrCrn(memberCrn);
                LOGGER.info("Found user for member Crn: '{}'.", memberCrn);
                break;
            case MACHINE_USER:
                actor.setMachineUserNameOrCrn(memberCrn);
                LOGGER.info("Found machine user for member Crn: '{}'.", memberCrn);
                break;
            default:
                throw new IllegalArgumentException(String.format("memberCrn %s is not a USER or MACHINE_USER", memberCrn));
        }

        try {
            AddMemberToGroupRequest.Builder addMemberToGroupRequest = AddMemberToGroupRequest.newBuilder()
                    .setMember(actor.build())
                    .setGroupNameOrCrn(groupName);
            AddMemberToGroupResponse addMemberToGroupResponse = newStub(requestId).addMemberToGroup(addMemberToGroupRequest.build());
            LOGGER.info("User '{}' has been assigned to the '{}' group successfully.", addMemberToGroupResponse.getMemberCrn(), groupName);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.NOT_FOUND.getCode())) {
                LOGGER.info("User group '{}' not found or has already been deleted.", groupName);
            } else if (e.getStatus().getCode().equals(Status.ALREADY_EXISTS.getCode())) {
                LOGGER.info("User '{}' for group '{}' has already assigned.", memberCrn, groupName);
            } else {
                throw e;
            }
        }
    }

    /**
     * Remove member from the selected user group if it is exist.
     *
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @param groupName the group where user is going to be assigned
     * @param memberCrn member (e.g., user) CRN
     */
    public void removeMemberFromGroup(String requestId, String accountId, String groupName, String memberCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(groupName, "groupName should not be null.");
        checkNotNull(memberCrn, "memberCrn should not be null.");
        validateAccountIdWithWarning(accountId);

        Crn crn = Crn.safeFromString(memberCrn);
        Actor.Builder actor = Actor.newBuilder().setAccountId(accountId);
        switch (crn.getResourceType()) {
            case USER:
                actor.setUserIdOrCrn(memberCrn);
                LOGGER.info("Found user for member Crn: '{}'.", memberCrn);
                break;
            case MACHINE_USER:
                actor.setMachineUserNameOrCrn(memberCrn);
                LOGGER.info("Found machine user for member Crn: '{}'.", memberCrn);
                break;
            default:
                throw new IllegalArgumentException(String.format("memberCrn %s is not a USER or MACHINE_USER", memberCrn));
        }

        try {
            RemoveMemberFromGroupRequest.Builder removeMemberFromGroupRequest = RemoveMemberFromGroupRequest.newBuilder()
                    .setMember(actor.build())
                    .setGroupNameOrCrn(groupName);
            RemoveMemberFromGroupResponse removeMemberFromGroupResponse = newStub(requestId).removeMemberFromGroup(removeMemberFromGroupRequest.build());
            LOGGER.info("User '{}' has been removed from the '{}' group successfully.", removeMemberFromGroupResponse.getMemberCrn(), groupName);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.NOT_FOUND.getCode())) {
                LOGGER.info("User group '{}' or user '{}' not found or has already been deleted.", groupName, memberCrn);
            } else {
                throw e;
            }
        }
    }

    /**
     * List members from the selected user group if it is exist.
     *
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @param groupName the group where user is going to be assigned
     * @return list of user group member CRNs or NULL if the user group does not exist.
     */
    public List<String> listMembersFromGroup(String requestId, String accountId, String groupName) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(groupName, "groupName should not be null.");
        validateAccountIdWithWarning(accountId);

        try {
            ListGroupMembersRequest.Builder listGroupMembersRequest = ListGroupMembersRequest.newBuilder()
                    .setGroupNameOrCrn(groupName)
                    .setAccountId(accountId)
                    .setIncludeDeleted(false);
            ListGroupMembersResponse listGroupMembersResponse;
            List<String> members = new ArrayList<>();
            do {
                listGroupMembersResponse = newStub(requestId).listGroupMembers(listGroupMembersRequest.build());
                for (int i = 0; i < listGroupMembersResponse.getMemberCrnCount(); i++) {
                    String memberCrn = listGroupMembersResponse.getMemberCrn(i);
                    members.add(memberCrn);
                }
                listGroupMembersRequest.setPageToken(listGroupMembersResponse.getNextPageToken());
            } while (listGroupMembersResponse.hasNextPageToken());
            LOGGER.info("User group '{}' contains members: [{}]", groupName, members);
            return members;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.NOT_FOUND.getCode())) {
                LOGGER.info("User group '{}' not found or has already been deleted.", groupName);
                return null;
            } else {
                throw e;
            }
        }
    }

    /**
     * Wraps calls to ListGroups with an Account ID.
     *
     * @param requestId          the request ID for the request
     * @param accountId          the account ID
     * @param groupNameOrCrnList the groups to list. if null or empty then all groups will be listed
     * @return the list of groups
     */
    public List<Group> listGroups(String requestId, String accountId, List<String> groupNameOrCrnList) {
        checkNotNull(requestId, "requestId should not be null.");
        validateAccountIdWithWarning(accountId);

        List<Group> groups = new ArrayList<>();

        ListGroupsRequest.Builder requestBuilder = ListGroupsRequest.newBuilder()
                .setAccountId(accountId)
                .setPageSize(umsClientConfig.getListGroupsPageSize());

        if (groupNameOrCrnList != null && !groupNameOrCrnList.isEmpty()) {
            requestBuilder.addAllGroupNameOrCrn(groupNameOrCrnList);
        }

        ListGroupsResponse response;
        do {
            response = newStub(requestId).listGroups(requestBuilder.build());
            groups.addAll(response.getGroupList());
            requestBuilder.setPageToken(response.getNextPageToken());
        } while (response.hasNextPageToken());
        return groups;
    }

    /**
     * Wraps calls to ListGroupsForMember with an Account ID and member CRN.
     *
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @param memberCrn member (e.g., user) CRN for which groups are fetched.
     * @return the list of group CRNs
     */
    public List<String> listGroupsForMembers(String requestId, String accountId, String memberCrn) {
        validateAccountIdWithWarning(accountId);
        checkNotNull(memberCrn, "memberCrn should not be null.");

        Crn crn = Crn.safeFromString(memberCrn);
        Actor.Builder actor = Actor.newBuilder().setAccountId(accountId);
        switch (crn.getResourceType()) {
            case USER:
                actor.setUserIdOrCrn(memberCrn);
                break;
            case MACHINE_USER:
                actor.setMachineUserNameOrCrn(memberCrn);
                break;
            default:
                throw new IllegalArgumentException(String.format("memberCrn %s is not a USER or MACHINE_USER", memberCrn));
        }
        ListGroupsForMemberRequest.Builder request = ListGroupsForMemberRequest.newBuilder()
                .setMember(actor.build());

        ListGroupsForMemberResponse response;
        List<String> groups = new ArrayList<>();
        do {
            response = newStub(requestId).listGroupsForMember(request.build());
            for (int i = 0; i < response.getGroupCrnCount(); i++) {
                String grpCRN = response.getGroupCrn(i);
                groups.add(grpCRN);
            }
            request.setPageToken(response.getNextPageToken());
        } while (response.hasNextPageToken());

        return groups;
    }

    /**
     * Wraps a call to getUser.
     *
     * @param requestId the request ID for the request
     * @param userCrn   the user CRN
     * @return the user
     */
    public User getUser(String requestId, String userCrn) {
        String accountId = Crn.fromString(userCrn).getAccountId();
        validateAccountIdWithWarning(accountId);
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        return newStub(requestId).getUser(
                GetUserRequest.newBuilder()
                        .setAccountId(accountId)
                        .setUserIdOrCrn(userCrn)
                        .build()
        ).getUser();
    }

    /**
     * Wraps a call to ListUsers with crn filter, therefore a single response is expected.
     *
     * @param requestId the request ID for the request
     * @param userCrn   the user CRN
     * @return the user
     */
    public User getUserWithList(String requestId, String userCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        Crn crn = Crn.fromString(userCrn);
        String accountId = crn.getAccountId();
        validateAccountIdWithWarning(accountId);

        List<User> users = newStub(requestId).listUsers(
                ListUsersRequest.newBuilder()
                        .setAccountId(accountId)
                        .addUserIdOrCrn(userCrn)
                        .build()
        ).getUserList();
        checkSingleUserResponse(users, crn.getResource());
        return users.get(0);
    }

    /**
     * Wraps calls to ListUsers with an Account ID.
     *
     * @param requestId       the request ID for the request
     * @param accountId       the account ID
     * @param userIdOrCrnList a list of users to list. If null or empty then all users will be listed
     * @return the list of users
     */
    public List<User> listUsers(String requestId, String accountId, List<String> userIdOrCrnList) {
        checkNotNull(requestId, "requestId should not be null.");
        validateAccountIdWithWarning(accountId);

        List<User> users = new ArrayList<>();

        ListUsersRequest.Builder requestBuilder = ListUsersRequest.newBuilder()
                .setAccountId(accountId)
                .setPageSize(umsClientConfig.getListUsersPageSize());

        if (userIdOrCrnList != null && !userIdOrCrnList.isEmpty()) {
            requestBuilder.addAllUserIdOrCrn(userIdOrCrnList);
        }

        ListUsersResponse response;
        do {
            response = newStub(requestId).listUsers(requestBuilder.build());
            users.addAll(response.getUserList());
            requestBuilder.setPageToken(response.getNextPageToken());
        } while (response.hasNextPageToken());
        return users;
    }

    public MachineUser getMachineUser(String requestId, String userCrn, String accountId) {
        return getMachineUserForUser(requestId, userCrn, accountId, userCrn, true, true);
    }

    public MachineUser getMachineUserForUser(String requestId, String userCrn, String accountId, String machineUserName,
            boolean includeWorkloadMachineUser, boolean includeInternal) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        Crn crn = Crn.fromString(userCrn);
        validateAccountIdWithWarning(accountId);
        List<MachineUser> machineUsers = newStub(requestId).listMachineUsers(
                ListMachineUsersRequest.newBuilder()
                        .setAccountId(accountId)
                        .addMachineUserNameOrCrn(machineUserName)
                        .setIncludeWorkloadMachineUsers(includeWorkloadMachineUser)
                        .setIncludeInternal(includeInternal)
                        .build()
        ).getMachineUserList();
        checkSingleUserResponse(machineUsers, crn.getResource());
        return machineUsers.get(0);
    }

    /**
     * Wraps calls to ListMachineUsers with an Account ID.
     *
     * @param requestId                   the request ID for the request
     * @param accountId                   the account ID
     * @param machineUserNameOrCrnList    a list of users to list. If null or empty then all users will be listed
     * @param includeInternal             whether to include internal machine users
     * @param includeWorkloadMachineUsers whether to include workload machine users
     * @return the list of machine users
     */
    public List<MachineUser> listMachineUsers(
            String requestId, String accountId, List<String> machineUserNameOrCrnList,
            boolean includeInternal, boolean includeWorkloadMachineUsers) {

        checkNotNull(requestId, "requestId should not be null.");
        validateAccountIdWithWarning(accountId);

        List<MachineUser> machineUsers = new ArrayList<>();

        ListMachineUsersRequest.Builder requestBuilder = ListMachineUsersRequest.newBuilder()
                .setAccountId(accountId)
                .setIncludeInternal(includeInternal)
                .setIncludeWorkloadMachineUsers(includeWorkloadMachineUsers)
                .setPageSize(umsClientConfig.getListMachineUsersPageSize());

        if (machineUserNameOrCrnList != null && !machineUserNameOrCrnList.isEmpty()) {
            requestBuilder.addAllMachineUserNameOrCrn(machineUserNameOrCrnList);
        }

        ListMachineUsersResponse response;
        do {
            response = newStub(requestId).listMachineUsers(requestBuilder.build());
            machineUsers.addAll(response.getMachineUserList());
            requestBuilder.setPageToken(response.getNextPageToken());
        } while (response.hasNextPageToken());
        return machineUsers;
    }

    /**
     * Create new machine user - only if it does not exist (returns the machine user crn if the user newly created)
     *
     * @param requestId       id of the request
     * @param userCrn         actor useridentifier
     * @param machineUserName machine user name that will be created
     */
    public Optional<String> createMachineUser(String requestId, String userCrn, String accountId, String machineUserName) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        checkNotNull(machineUserName, "machineUserName should not be null.");
        validateAccountIdWithWarning(accountId);
        Optional<String> emptyResponse = Optional.empty();
        try {
            UserManagementProto.CreateWorkloadMachineUserResponse response = newStub(requestId).createWorkloadMachineUser(
                    UserManagementProto.CreateWorkloadMachineUserRequest.newBuilder()
                            .setAccountId(accountId)
                            .setMachineUserName(machineUserName)
                            .build());
            LOGGER.info("Machine user created: {}.", response.getMachineUser().getCrn());
            if (response.hasMachineUser()) {
                return Optional.of(response.getMachineUser().getCrn());
            }
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(
                    io.grpc.Status.ALREADY_EXISTS.getCode())) {
                LOGGER.info("Machine user already exists.");
            } else {
                throw e;
            }
        }
        return emptyResponse;
    }

    public MachineUser getOrCreateMachineUserWithoutAccessKey(String requestId, String accountId, String machineUserName) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(machineUserName, "machineUserName should not be null.");
        validateAccountIdWithWarning(accountId);
        //Idempotent api creates only if not existing.
        UserManagementProto.CreateWorkloadMachineUserResponse response = newStub(requestId).createWorkloadMachineUser(
                UserManagementProto.CreateWorkloadMachineUserRequest.newBuilder()
                        .setAccountId(accountId)
                        .setMachineUserName(machineUserName)
                        .setGenerateAccessKey(NullableScalarTypeProto.BoolValue.newBuilder().setValue(false))
                        .build());
        LOGGER.info("Machine user created: {}.", response.getMachineUser());
        return response.getMachineUser();
    }

    /**
     * Remove machine user
     *
     * @param requestId       id of the request
     * @param userCrn         actor identifier
     * @param machineUserName machine user to remove
     */
    public void deleteMachineUser(String requestId, String userCrn, String accountId, String machineUserName) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        checkNotNull(machineUserName, "machineUserName should not be null.");
        validateAccountIdWithWarning(accountId);
        try {
            newStub(requestId).deleteMachineUser(
                    UserManagementProto.DeleteMachineUserRequest.newBuilder()
                            .setAccountId(accountId)
                            .setMachineUserNameOrCrn(machineUserName)
                            .build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(
                    Status.NOT_FOUND.getCode())) {
                LOGGER.info("Machine user not found.");
            } else {
                throw e;
            }
        }
    }

    /**
     * Wraps a call to ListServicePrincipalCloudIdentities.
     *
     * @param requestId      the request ID for the request
     * @param accountId      the account id
     * @param environmentCrn the environment crn
     * @return the list of service principal cloud identities
     */
    public ListServicePrincipalCloudIdentitiesResponse listServicePrincipalCloudIdentities(
            String requestId, String accountId, String environmentCrn, Optional<PagingProto.PageToken> pageToken) {
        validateAccountIdWithWarning(accountId);
        ListServicePrincipalCloudIdentitiesRequest.Builder requestBuilder = ListServicePrincipalCloudIdentitiesRequest.newBuilder()
                .setAccountId(accountId)
                .setEnvironmentCrn(environmentCrn)
                .setPageSize(umsClientConfig.getListServicePrincipalCloudIdentitiesPageSize());
        if (pageToken.isPresent()) {
            requestBuilder.setPageToken(pageToken.get());
        }
        return newStub(requestId).listServicePrincipalCloudIdentities(requestBuilder.build());
    }

    private <T> void checkSingleUserResponse(List<T> users, String crnResource) {
        if (users.size() < 1) {
            throw new UmsAuthenticationException(String.format("No user found in UMS system: %s", crnResource));
        } else if (users.size() > 1) {
            throw new UmsAuthenticationException(String.format("Multiple users found in UMS system: %s", crnResource));
        }
    }

    public UserManagementProto.ListAssignedResourceRolesResponse listAssignedResourceRoles(String requestId, String assigneeCrn) {
        return newStub(requestId).listAssignedResourceRoles(UserManagementProto.ListAssignedResourceRolesRequest.newBuilder()
                .setAssignee(getAssignee(assigneeCrn))
                .build());
    }

    /**
     * Assign a resource role to an assignee
     *
     * @param requestId       id of the request
     * @param assigneeCrn     user CRN who is going to be own the selected role on the resource
     * @param resourceCrn     resource CRN where the resource role is going to be assigned to the user
     * @param resourceRoleCrn selected resource role CRN for user
     * @return AssignResourceRoleResponse
     */
    public UserManagementProto.AssignResourceRoleResponse assignResourceRole(String requestId, String assigneeCrn, String resourceCrn,
            String resourceRoleCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(assigneeCrn, "assigneeCrn should not be null.");
        checkNotNull(resourceCrn, "resourceCrn should not be null.");
        checkNotNull(resourceRoleCrn, "resourceRoleCrn should not be null.");

        UserManagementProto.AssignResourceRoleResponse assignResourceRoleResponse =
                newStub(requestId).assignResourceRole(UserManagementProto.AssignResourceRoleRequest.newBuilder()
                        .setAssignee(getAssignee(assigneeCrn))
                        .setResourceCrn(resourceCrn)
                        .setResourceRoleCrn(resourceRoleCrn)
                        .build());
        return assignResourceRoleResponse;
    }

    /**
     * Unassign a resource role from an assignee
     *
     * @param requestId       id of the request
     * @param assigneeCrn     user CRN whom resource role is going to be revoked
     * @param resourceCrn     resource CRN where the resource role is going to be revoked from the user
     * @param resourceRoleCrn selected resource role CRN for user
     * @return UnassignResourceRoleResponse
     */
    public UserManagementProto.UnassignResourceRoleResponse unassignResourceRole(String requestId, String assigneeCrn, String resourceCrn,
            String resourceRoleCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(assigneeCrn, "assigneeCrn should not be null.");
        checkNotNull(resourceCrn, "resourceCrn should not be null.");
        checkNotNull(resourceRoleCrn, "resourceRoleCrn should not be null.");

        UserManagementProto.UnassignResourceRoleResponse unassignResourceRoleResponse =
                newStub(requestId).unassignResourceRole(UserManagementProto.UnassignResourceRoleRequest.newBuilder()
                        .setAssignee(getAssignee(assigneeCrn))
                        .setResourceCrn(resourceCrn)
                        .setResourceRoleCrn(resourceRoleCrn)
                        .build());
        return unassignResourceRoleResponse;
    }

    /**
     * Add a role (to machine user) - if role does not exist
     *
     * @param requestId      id of the request
     * @param userCrn        actor user (account & assignee)
     * @param machineUserCrn machine user identifier
     * @param roleCrn        role identifier
     */
    public void assignMachineUserRole(String requestId, String userCrn, String accountId, String machineUserCrn, String roleCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        checkNotNull(machineUserCrn, "machineUserCrn should not be null.");
        checkNotNull(roleCrn, "roleCrn should not be null.");
        validateAccountIdWithWarning(accountId);
        try {
            newStub(requestId).assignRole(
                    UserManagementProto.AssignRoleRequest.newBuilder()
                            .setActor(UserManagementProto.Actor.newBuilder()
                                    .setAccountId(accountId)
                                    .setMachineUserNameOrCrn(machineUserCrn)
                                    .build())
                            .setAssignee(UserManagementProto.Assignee.newBuilder()
                                    .setAccountId(accountId)
                                    .setMachineUserNameOrCrn(machineUserCrn)
                                    .build())
                            .setRoleNameOrCrn(roleCrn)
                            .build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(
                    Status.ALREADY_EXISTS.getCode())) {
                LOGGER.info("Role ({}) for machine user ({}) already assigned.", roleCrn, machineUserCrn);
            } else {
                throw e;
            }
        }
    }

    /**
     * Add a resource role (to machine user) - if resource role does not exist
     *
     * @param requestId       id of the request
     * @param accountId       account id for user
     * @param machineUserCrn  machine user identifier
     * @param resourceRoleCrn resource role identifier
     * @param resourceCrn     resource identifier
     */
    public void assignMachineUserResourceRole(String requestId, String accountId, String machineUserCrn, String resourceRoleCrn,
            String resourceCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(machineUserCrn, "machineUserCrn should not be null.");
        checkNotNull(resourceRoleCrn, "resourceRoleCrn should not be null.");
        checkNotNull(resourceCrn, "resourceCrn should not be null.");
        validateAccountIdWithWarning(accountId);
        try {
            newStub(requestId).assignResourceRole(
                    UserManagementProto.AssignResourceRoleRequest.newBuilder()
                            .setActor(UserManagementProto.Actor.newBuilder()
                                    .setAccountId(accountId)
                                    .setMachineUserNameOrCrn(machineUserCrn)
                                    .build())
                            .setAssignee(UserManagementProto.Assignee.newBuilder()
                                    .setAccountId(accountId)
                                    .setMachineUserNameOrCrn(machineUserCrn)
                                    .build())
                            .setResourceRoleCrn(resourceRoleCrn)
                            .setResourceCrn(resourceCrn)
                            .build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(Status.ALREADY_EXISTS.getCode())) {
                LOGGER.info("Resource role ({}) for machine user ({}) and resource ({}) already assigned.", resourceRoleCrn, machineUserCrn, resourceCrn);
            } else {
                throw e;
            }
        }
    }

    /**
     * Remove a role (from machine user) - if role exists
     *
     * @param requestId      id of the request
     * @param accountId      account id for user
     * @param machineUserCrn machine user identifier
     * @param roleCrn        role identifier
     */
    public void unassignMachineUserRole(String requestId, String machineUserCrn, String roleCrn, String accountId) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(machineUserCrn, "machineUserCrn should not be null.");
        checkNotNull(roleCrn, "roleCrn should not be null.");
        validateAccountIdWithWarning(accountId);
        try {
            newStub(requestId).unassignRole(
                    UserManagementProto.UnassignRoleRequest.newBuilder()
                            .setActor(
                                    UserManagementProto.Actor.newBuilder()
                                            .setAccountId(accountId)
                                            .setMachineUserNameOrCrn(machineUserCrn)
                                            .build())
                            .setAssignee(
                                    UserManagementProto.Assignee.newBuilder()
                                            .setAccountId(accountId)
                                            .setMachineUserNameOrCrn(machineUserCrn)
                                            .build())
                            .setRoleNameOrCrn(roleCrn)
                            .build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode().equals(
                    Status.NOT_FOUND.getCode())) {
                LOGGER.info("Cannot find role ({}) for machine user ({}).", roleCrn, machineUserCrn);
            } else {
                throw e;
            }
        }
    }

    public List<UserManagementProto.ResourceAssignment> listAssigmentsOfUser(String requestId, String userCrn) {
        return newStub(requestId).getAssigneeAuthorizationInformation(UserManagementProto.GetAssigneeAuthorizationInformationRequest.newBuilder()
                        .setAssigneeCrn(userCrn)
                        .build())
                .getResourceAssignmentList();
    }

    public List<UserManagementProto.ResourceAssignee> listResourceAssigneesForResource(String requestId, String resourceCrn) {
        return newStub(requestId).listResourceAssignees(UserManagementProto.ListResourceAssigneesRequest.newBuilder()
                        .setAccountId(Crn.safeFromString(resourceCrn).getAccountId())
                        .setResourceCrn(resourceCrn)
                        .build())
                .getResourceAssigneeList();
    }

    public void notifyResourceDeleted(String requestId, String resourceCrn) {
        newStub(requestId).notifyResourceDeleted(UserManagementProto.NotifyResourceDeletedRequest.newBuilder()
                .setResourceCrn(resourceCrn)
                .build());
    }

    /**
     * Wraps a call to getAccount.
     *
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @return the account
     */
    public Account getAccount(String requestId, String accountId) {
        checkNotNull(requestId, "requestId should not be null.");
        warnIfAccountIdIsInternal(accountId);
        return newStub(requestId).getAccount(
                GetAccountRequest.newBuilder()
                        .setAccountId(accountId)
                        .build()
        ).getAccount();
    }

    /**
     * Wraps a call to setActorWorkloadPassword.
     *
     * @param requestId the request ID for the request
     * @param userCrn   the user CRN
     * @param password  the new Workload password
     * @return the ActorWorkloadCredentialsResponse
     */
    public UserManagementProto.SetActorWorkloadCredentialsResponse setActorWorkloadPassword(String requestId, String userCrn, String password) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        checkNotNull(password, "password should not be null.");
        UserManagementProto.SetActorWorkloadCredentialsResponse response = newStub(requestId).setActorWorkloadCredentials(
                UserManagementProto.SetActorWorkloadCredentialsRequest.newBuilder()
                        .setActorCrn(userCrn)
                        .setPassword(password)
                        .build()
        );
        response.getAllFields().forEach((key, value) -> {
            LOGGER.info("Workload '{}' credential value is '{}' after password update.", key, value);
        });
        return response;
    }

    /**
     * Wraps a call to getActorWorkloadCredentials.
     *
     * @param requestId the request ID for the request
     * @param userCrn   the user CRN
     * @return the ActorWorkloadCredentialsResponse
     */
    public UserManagementProto.GetActorWorkloadCredentialsResponse getActorWorkloadCredentials(String requestId, String userCrn) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        return newStub(requestId).getActorWorkloadCredentials(
                UserManagementProto.GetActorWorkloadCredentialsRequest.newBuilder()
                        .setActorCrn(userCrn)
                        .build()
        );
    }

    /**
     * Wraps a call to getRights
     *
     * @param requestId   the request ID for the request
     * @param actorCrn    the actor CRN
     * @param resourceCrn the user or machine user CRN
     * @return rights object for the user or machine user
     */
    public GetRightsResponse getRightsForUser(String requestId, String actorCrn, String resourceCrn) {
        if (resourceCrn == null) {
            resourceCrn = "*";
        }
        return newShortTimeoutStub(requestId).getRights(
                GetRightsRequest.newBuilder()
                        .setActorCrn(actorCrn)
                        .setResourceCrn(resourceCrn)
                        .build()
        );
    }

    /**
     * Wraps a call to listWorkloadAdministrationGroupsForMember.
     *
     * @param requestId the request ID for the request
     * @param memberCrn the member CRN
     * @return the workload administration groups for the member
     */
    public ListWorkloadAdministrationGroupsForMemberResponse listWorkloadAdministrationGroupsForMember(
            String requestId, String memberCrn, Optional<PagingProto.PageToken> pageToken) {
        ListWorkloadAdministrationGroupsForMemberRequest.Builder requestBuilder = ListWorkloadAdministrationGroupsForMemberRequest.newBuilder()
                .setPageSize(umsClientConfig.getListWorkloadAdministrationGroupsForMemberPageSize())
                .setMemberCrn(memberCrn);
        if (pageToken.isPresent()) {
            requestBuilder.setPageToken(pageToken.get());
        }
        return newStub(requestId).listWorkloadAdministrationGroupsForMember(requestBuilder.build());
    }

    /**
     * Wraps a call to create an access private key pair
     *
     * @param requestId       the request ID for the request
     * @param userCrn         the user CRN
     * @param machineUserName the machine user name
     * @return key creation response
     */
    CreateAccessKeyResponse createAccessPrivateKeyPair(String requestId, String userCrn, String accountId, String machineUserName) {
        return createAccessPrivateKeyPair(requestId, userCrn, accountId, machineUserName,
                UserManagementProto.AccessKeyType.Value.UNSET);
    }

    /**
     * Wraps a call to create an access private key pair with specified algorithm
     *
     * @param requestId       the request ID for the request
     * @param userCrn         the user CRN
     * @param machineUserName the machine user name
     * @param accessKeyType   accessKeyType
     * @return key creation response
     */
    CreateAccessKeyResponse createAccessPrivateKeyPair(String requestId, String userCrn, String accountId, String machineUserName,
            UserManagementProto.AccessKeyType.Value accessKeyType) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        checkNotNull(machineUserName, "machineUserName should not be null.");
        validateAccountIdWithWarning(accountId);
        CreateAccessKeyRequest.Builder builder = CreateAccessKeyRequest.newBuilder();
        builder.setAccountId(accountId)
                .setMachineUserNameOrCrn(machineUserName)
                .setInternal(true)
                .setType(accessKeyType);
        if (!UserManagementProto.AccessKeyType.Value.UNSET.equals(accessKeyType)) {
            builder.setType(accessKeyType);
        }
        return newStub(requestId).createAccessKey(builder.build());
    }

    /**
     * Get a list of access key CRN (keys owned by a machine user)
     *
     * @param requestId       id of the request
     * @param userCrn         actor that query the keys for the machine user
     * @param machineUserName machine user that owns the access keys
     * @return access key CRNs
     */
    public List<String> listMachineUserAccessKeys(String requestId, String userCrn, String accountId, String machineUserName) {
        return listMachineUserAccessKeys(requestId, userCrn, accountId, machineUserName, false);
    }

    /**
     * Get a list of access key CRN (keys owned by a machine user)
     *
     * @param requestId       id of the request
     * @param userCrn         actor that query the keys for the machine user
     * @param machineUserName machine user that owns the access keys
     * @param userAccessKeyId put access key id to the result instead of access key crns
     * @return access key CRNs (or access key ids)
     */
    public List<String> listMachineUserAccessKeys(String requestId, String userCrn, String accountId,
            String machineUserName, boolean userAccessKeyId) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(userCrn, "userCrn should not be null.");
        checkNotNull(machineUserName, "machineUserName should not be null.");
        warnIfAccountIdIsInternal(accountId);
        List<String> accessKeys = new ArrayList<>();
        UserManagementProto.ListAccessKeysRequest.Builder listAccessKeysRequestBuilder =
                UserManagementProto.ListAccessKeysRequest.newBuilder()
                        .setAccountId(accountId)
                        .setKeyAssignee(UserManagementProto.Actor.newBuilder()
                                .setAccountId(accountId)
                                .setMachineUserNameOrCrn(machineUserName)
                                .build());
        do {
            try {
                UserManagementProto.ListAccessKeysResponse listAccessKeysResponse =
                        newStub(requestId).listAccessKeys(listAccessKeysRequestBuilder.build());
                accessKeys.addAll(
                        listAccessKeysResponse.getAccessKeyList().stream()
                                .map(accessKeyObject -> {
                                    if (userAccessKeyId) {
                                        return accessKeyObject.getAccessKeyId();
                                    } else {
                                        return accessKeyObject.getCrn();
                                    }
                                })
                                .collect(Collectors.toList()));
                if (!listAccessKeysResponse.hasNextPageToken()) {
                    break;
                }
                listAccessKeysRequestBuilder.setPageToken(
                        listAccessKeysResponse.getNextPageToken());
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode().equals(io.grpc.Status.NOT_FOUND.getCode())) {
                    LOGGER.info("Machine user does not exist. Cannot list access keys.");
                    break;
                } else {
                    throw e;
                }
            }
        } while (true);
        LOGGER.info("Found {} access keys for the machine user {}", accessKeys.size(), machineUserName);
        return accessKeys;
    }

    /**
     * Delete access keys identified by CRNs
     *
     * @param requestId     id of the request
     * @param accessKeyCrns list of access key CRNs
     * @param accountId     account id for the user
     */
    void deleteAccessKeys(String requestId, List<String> accessKeyCrns, String accountId) {
        checkNotNull(requestId, "requestId should not be null.");
        warnIfAccountIdIsInternal(accountId);
        checkNotNull(accessKeyCrns, "accessKeyCrns should not be null.");
        accessKeyCrns.forEach(accessKeyCrn -> {
            try {
                LOGGER.info("Deleting access key {}...", accessKeyCrn);
                newStub(requestId).deleteAccessKey(UserManagementProto.DeleteAccessKeyRequest.newBuilder()
                        .setAccountId(accountId)
                        .setAccessKeyIdOrCrn(accessKeyCrn)
                        .build());
                LOGGER.info("Access key {} deleted.", accessKeyCrn);
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode().equals(io.grpc.Status.NOT_FOUND.getCode())) {
                    LOGGER.info("Access key {} does not exist.", accessKeyCrn);
                } else {
                    throw e;
                }
            }
        });
    }

    /**
     * Retrieves event generation ids for an account
     *
     * @param requestId id of the request
     * @param accountId id of the account
     */
    UserManagementProto.GetEventGenerationIdsResponse getEventGenerationIds(String requestId, String accountId) {
        checkNotNull(requestId, "requestId should not be null.");

        return newStub(requestId).getEventGenerationIds(UserManagementProto.GetEventGenerationIdsRequest.newBuilder()
                .setAccountId(accountId)
                .build());
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors (short timeout).
     *
     * @param requestId the request ID
     * @return the stub
     */
    private UserManagementBlockingStub newShortTimeoutStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        return UserManagementGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(umsClientConfig.getGrpcShortTimeoutSec()),
                        GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()),
                        new CallingServiceNameInterceptor(umsClientConfig.getCallingServiceName()));
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private UserManagementBlockingStub newStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        return UserManagementGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(umsClientConfig.getGrpcTimeoutSec()),
                        GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()),
                        new CallingServiceNameInterceptor(umsClientConfig.getCallingServiceName()));
    }

    /**
     * Queries the metadata file used to configure SSO authentication on clusters.
     *
     * @param requestId the Request ID
     * @param accountId the account ID
     * @return metadata as string
     */
    public String getIdentityProviderMetadataXml(String requestId, String accountId) {
        validateAccountIdWithWarning(accountId);
        GetIdPMetadataForWorkloadSSORequest request =
                GetIdPMetadataForWorkloadSSORequest.newBuilder()
                        .setAccountId(accountId)
                        .build();
        GetIdPMetadataForWorkloadSSOResponse response = newStub(requestId).getIdPMetadataForWorkloadSSO(request);
        return response.getMetadata();
    }

    public UserManagementProto.SetWorkloadAdministrationGroupNameResponse setWorkloadAdministrationGroupName(String requestId, String accountId,
            String right, String resource) {
        validateAccountIdWithWarning(accountId);
        checkNotNull(right, "right should not be null.");
        checkNotNull(resource, "resource should not be null.");
        return newStub(requestId).setWorkloadAdministrationGroupName(UserManagementProto.SetWorkloadAdministrationGroupNameRequest.newBuilder()
                .setAccountId(accountId)
                .setRightName(right)
                .setResource(resource)
                .build());
    }

    public UserManagementProto.GetWorkloadAdministrationGroupNameResponse getWorkloadAdministrationGroupName(String requestId, String accountId,
            String right, String resource) {
        validateAccountIdWithWarning(accountId);
        checkNotNull(right, "right should not be null.");
        checkNotNull(resource, "resource should not be null.");
        return newStub(requestId).getWorkloadAdministrationGroupName(UserManagementProto.GetWorkloadAdministrationGroupNameRequest.newBuilder()
                .setAccountId(accountId)
                .setRightName(right)
                .setResource(resource)
                .build());
    }

    public UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse deleteWorkloadAdministrationGroupName(String requestId, String accountId,
            String right, String resource) {
        validateAccountIdWithWarning(accountId);
        checkNotNull(right, "right should not be null.");
        checkNotNull(resource, "resource should not be null.");
        return newStub(requestId).deleteWorkloadAdministrationGroupName(UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest.newBuilder()
                .setAccountId(accountId)
                .setRightName(right)
                .setResource(resource)
                .build());
    }

    /**
     * Wraps calls to ListWorkloadAdministrationGroups with an Account ID.
     *
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @return the list of workload administration groups
     */
    public List<WorkloadAdministrationGroup> listWorkloadAdministrationGroups(String requestId, String accountId) {
        checkNotNull(requestId, "requestId should not be null.");
        validateAccountIdWithWarning(accountId);

        List<WorkloadAdministrationGroup> wags = new ArrayList<>();

        ListWorkloadAdministrationGroupsRequest.Builder requestBuilder = ListWorkloadAdministrationGroupsRequest.newBuilder()
                .setAccountId(accountId)
                .setPageSize(umsClientConfig.getListWorkloadAdministrationGroupsPageSize());

        ListWorkloadAdministrationGroupsResponse response;
        do {
            response = newStub(requestId).listWorkloadAdministrationGroups(requestBuilder.build());
            wags.addAll(response.getWorkloadAdministrationGroupList());
            requestBuilder.setPageToken(response.getNextPageToken());
        } while (response.hasNextPageToken());
        return wags;
    }

    /**
     * Wraps calls to Assignee with an Assignee CRN.
     *
     * @param assigneeCrn the assignee CRN
     * @return the Assignee based on the provided CRN
     */
    private UserManagementProto.Assignee getAssignee(String assigneeCrn) {
        checkNotNull(assigneeCrn, "assigneeCrn should not be null.");
        Crn crn = Crn.safeFromString(assigneeCrn);
        String accountId = crn.getAccountId();
        validateAccountIdWithWarning(accountId);

        UserManagementProto.Assignee.Builder assignee = UserManagementProto.Assignee.newBuilder().setAccountId(accountId);
        switch (crn.getResourceType()) {
            case USER:
                assignee.setUserIdOrCrn(assigneeCrn);
                break;
            case MACHINE_USER:
                assignee.setMachineUserNameOrCrn(assigneeCrn);
                break;
            case GROUP:
                assignee.setGroupNameOrCrn(assigneeCrn);
                break;
            default:
                throw new IllegalArgumentException(String.format("assigneeCrn '%s' is not a USER, MACHINE_USER or a GROUP", assigneeCrn));
        }
        return assignee.build();
    }

    /**
     * Retrieves user sync state model from the UMS.
     *
     * @param requestId       the request ID for the request
     * @param accountId       the account ID
     * @param rightsChecks    list of rights checks for resources. A List is used to preserve order.
     * @param skipCredentials whether to skip including credentials in the response
     * @return the user sync state model
     */
    public GetUserSyncStateModelResponse getUserSyncStateModel(
            String requestId, String accountId, List<RightsCheck> rightsChecks, boolean skipCredentials) {
        validateAccountIdWithWarning(accountId);
        GetUserSyncStateModelRequest request = GetUserSyncStateModelRequest.newBuilder()
                .setAccountId(accountId)
                .addAllRightsCheck(rightsChecks)
                .setSkipCredentials(skipCredentials)
                .build();
        return newStub(requestId).getUserSyncStateModel(request);
    }

    public Set<String> listResourceRoles(String requestId, String accountId) {
        UserManagementBlockingStub stub = newStub(requestId);
        UserManagementProto.ListResourceRolesRequest.Builder requestBuilder = UserManagementProto.ListResourceRolesRequest.newBuilder().setAccountId(accountId);
        UserManagementProto.ListResourceRolesResponse response;
        Set<String> resourceRoles = Sets.newHashSet();
        do {
            response = stub.listResourceRoles(requestBuilder.build());
            resourceRoles.addAll(response.getResourceRoleList().stream().map(UserManagementProto.ResourceRole::getCrn).collect(Collectors.toSet()));
            requestBuilder.setPageToken(response.getNextPageToken());
        } while (response.hasNextPageToken());
        return resourceRoles;
    }

    public Set<String> listRoles(String requestId, String accountId) {
        UserManagementBlockingStub stub = newStub(requestId);
        UserManagementProto.ListRolesRequest.Builder requestBuilder = UserManagementProto.ListRolesRequest.newBuilder().setAccountId(accountId);
        UserManagementProto.ListRolesResponse response;
        Set<String> roles = Sets.newHashSet();
        do {
            response = stub.listRoles(requestBuilder.build());
            roles.addAll(response.getRoleList().stream().map(UserManagementProto.Role::getCrn).collect(Collectors.toSet()));
            requestBuilder.setPageToken(response.getNextPageToken());
        } while (response.hasNextPageToken());
        return roles;
    }

    private void validateAccountIdWithWarning(String accountId) {
        checkNotNull(accountId, "accountId should not be null.");
        warnIfAccountIdIsInternal(accountId);
    }
}

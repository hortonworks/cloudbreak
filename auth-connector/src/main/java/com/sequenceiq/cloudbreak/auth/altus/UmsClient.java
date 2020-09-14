package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.common.paging.PagingProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc;
import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc.UserManagementBlockingStub;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Actor;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSORequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetIdPMetadataForWorkloadSSOResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
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
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadAdministrationGroup;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsAuthenticationException;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class UmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsClient.class);

    private final ManagedChannel channel;

    private final String actorCrn;

    private final UmsClientConfig umsClientConfig;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param actorCrn the actor CRN.
     */
    UmsClient(ManagedChannel channel,
            String actorCrn,
            UmsClientConfig umsClientConfig) {
        this.channel = checkNotNull(channel);
        this.actorCrn = checkNotNull(actorCrn);
        this.umsClientConfig = checkNotNull(umsClientConfig);
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
        checkNotNull(requestId);
        checkNotNull(accountId);

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
        checkNotNull(accountId);
        checkNotNull(memberCrn);

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
        checkNotNull(requestId);
        checkNotNull(userCrn);
        return newStub(requestId).getUser(
                GetUserRequest.newBuilder()
                        .setAccountId(Crn.fromString(userCrn).getAccountId())
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
        checkNotNull(requestId);
        checkNotNull(userCrn);
        Crn crn = Crn.fromString(userCrn);
        List<User> users = newStub(requestId).listUsers(
                ListUsersRequest.newBuilder()
                        .setAccountId(crn.getAccountId())
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
        checkNotNull(requestId);
        checkNotNull(accountId);

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
        return getMachineUserForUser(requestId, userCrn, accountId, userCrn);
    }

    public MachineUser getMachineUserForUser(String requestId, String userCrn, String accountId, String machineUserName) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        Crn crn = Crn.fromString(userCrn);
        List<MachineUser> machineUsers = newStub(requestId).listMachineUsers(
                ListMachineUsersRequest.newBuilder()
                        .setAccountId(accountId)
                        .addMachineUserNameOrCrn(machineUserName)
                        .build()
        ).getMachineUserList();
        checkSingleUserResponse(machineUsers, crn.getResource());
        return machineUsers.get(0);
    }

    /**
     * Wraps calls to ListMachineUsers with an Account ID.
     *
     * @param requestId                the request ID for the request
     * @param accountId                the account ID
     * @param machineUserNameOrCrnList a list of users to list. If null or empty then all users will be listed
     * @param includeInternal          whether to include internal machine users
     * @return the list of machine users
     */
    public List<MachineUser> listMachineUsers(
            String requestId, String accountId, List<String> machineUserNameOrCrnList, boolean includeInternal) {

        checkNotNull(requestId);
        checkNotNull(accountId);

        List<MachineUser> machineUsers = new ArrayList<>();

        ListMachineUsersRequest.Builder requestBuilder = ListMachineUsersRequest.newBuilder()
                .setAccountId(accountId)
                .setIncludeInternal(includeInternal)
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
        checkNotNull(requestId);
        checkNotNull(userCrn);
        checkNotNull(machineUserName);
        Optional<String> emptyResponse = Optional.empty();
        try {
            UserManagementProto.CreateMachineUserResponse response = newStub(requestId).createMachineUser(
                    UserManagementProto.CreateMachineUserRequest.newBuilder()
                            .setAccountId(accountId)
                            .setMachineUserName(machineUserName)
                            .build());
            LOGGER.info("Machine user created: {}.", response.getMachineUser().getCrn());
            if (response.getMachineUser() != null) {
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

    /**
     * Remove machine user
     *
     * @param requestId       id of the request
     * @param userCrn         actor identifier
     * @param machineUserName machine user to remove
     */
    public void deleteMachineUser(String requestId, String userCrn, String machineUserName) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        checkNotNull(machineUserName);
        try {
            newStub(requestId).deleteMachineUser(
                    UserManagementProto.DeleteMachineUserRequest.newBuilder()
                            .setAccountId(Crn.fromString(userCrn).getAccountId())
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
     * @param requestId   the request ID for the request
     * @param accountId   the account id
     * @param environmentCrn   the environment crn
     * @return the list of service principal cloud identities
     */
    public ListServicePrincipalCloudIdentitiesResponse listServicePrincipalCloudIdentities(
            String requestId, String accountId, String environmentCrn, Optional<PagingProto.PageToken> pageToken) {
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

    public void assignResourceRole(String requestId, String userCrn, String resourceCrn, String resourceRoleCrn) {
        newStub(requestId).assignResourceRole(UserManagementProto.AssignResourceRoleRequest.newBuilder()
                .setAssignee(getAssignee(userCrn))
                .setResourceCrn(resourceCrn)
                .setResourceRoleCrn(resourceRoleCrn)
                .build());
    }

    public void unassignResourceRole(String requestId, String userCrn, String resourceCrn, String resourceRoleCrn) {
        newStub(requestId).unassignResourceRole(UserManagementProto.UnassignResourceRoleRequest.newBuilder()
                .setAssignee(getAssignee(userCrn))
                .setResourceCrn(resourceCrn)
                .setResourceRoleCrn(resourceRoleCrn)
                .build());
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
        checkNotNull(requestId);
        checkNotNull(userCrn);
        checkNotNull(machineUserCrn);
        checkNotNull(roleCrn);
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
     * Remove a role (from machine user) - if role exists
     *
     * @param requestId      id of the request
     * @param userCrn        actor user (account & assignee)
     * @param machineUserCrn machine user identifier
     * @param roleCrn        role identifier
     */
    public void unassignMachineUserRole(String requestId, String userCrn, String machineUserCrn, String roleCrn) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        checkNotNull(machineUserCrn);
        checkNotNull(roleCrn);
        try {
            newStub(requestId).unassignRole(
                    UserManagementProto.UnassignRoleRequest.newBuilder()
                            .setActor(
                                    UserManagementProto.Actor.newBuilder()
                                            .setAccountId(Crn.fromString(userCrn).getAccountId())
                                            .setMachineUserNameOrCrn(machineUserCrn)
                                            .build())
                            .setAssignee(
                                    UserManagementProto.Assignee.newBuilder()
                                            .setAccountId(Crn.fromString(userCrn).getAccountId())
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
                .setAccountId(Crn.fromString(resourceCrn).getAccountId())
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
        checkNotNull(requestId);
        checkNotNull(accountId);
        return newStub(requestId).getAccount(
                GetAccountRequest.newBuilder()
                        .setAccountId(accountId)
                        .build()
        ).getAccount();
    }

    /**
     * Wraps a call to getActorWorkloadCredentials.
     *
     * @param requestId the request ID for the request
     * @param userCrn   the user CRN
     * @return the ActorWorkloadCredentialsResponse
     */
    public GetActorWorkloadCredentialsResponse getActorWorkloadCredentials(String requestId, String userCrn) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        return newStub(requestId).getActorWorkloadCredentials(
                GetActorWorkloadCredentialsRequest.newBuilder()
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
        return newStub(requestId).getRights(
                GetRightsRequest.newBuilder()
                        .setActorCrn(actorCrn)
                        .setResourceCrn(resourceCrn)
                        .build()
        );
    }

    /**
     * Wraps a call to listWorkloadAdministrationGroupsForMember.
     *
     * @param requestId   the request ID for the request
     * @param memberCrn   the member CRN
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
        checkNotNull(requestId);
        checkNotNull(userCrn);
        checkNotNull(machineUserName);
        CreateAccessKeyRequest.Builder builder = CreateAccessKeyRequest.newBuilder();
        builder.setAccountId(accountId)
                .setMachineUserNameOrCrn(machineUserName)
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
        checkNotNull(requestId);
        checkNotNull(userCrn);
        checkNotNull(machineUserName);
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
                                .map(UserManagementProto.AccessKey::getCrn)
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
     * @param actorCrn      user that executes the deletion
     */
    void deleteAccessKeys(String requestId, List<String> accessKeyCrns, String actorCrn) {
        checkNotNull(requestId);
        checkNotNull(actorCrn);
        checkNotNull(accessKeyCrns);
        accessKeyCrns.forEach(accessKeyCrn -> {
            try {
                LOGGER.info("Deleting access key {}...", accessKeyCrn);
                newStub(requestId).deleteAccessKey(UserManagementProto.DeleteAccessKeyRequest.newBuilder()
                        .setAccountId(Crn.fromString(actorCrn).getAccountId())
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
        checkNotNull(requestId);
        checkNotNull(accountId);

        return newStub(requestId).getEventGenerationIds(UserManagementProto.GetEventGenerationIdsRequest.newBuilder()
                .setAccountId(accountId)
                .build());
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private UserManagementBlockingStub newStub(String requestId) {
        checkNotNull(requestId);
        return UserManagementGrpc.newBlockingStub(channel)
                .withInterceptors(new AltusMetadataInterceptor(requestId, actorCrn));
    }

    /**
     * Queries the metadata file used to configure SSO authentication on clusters.
     *
     * @param requestId the Request ID
     * @param accountId the account ID
     * @return metadata as string
     */
    public String getIdentityProviderMetadataXml(String requestId, String accountId) {
        checkNotNull(accountId);

        GetIdPMetadataForWorkloadSSORequest request =
                GetIdPMetadataForWorkloadSSORequest.newBuilder()
                        .setAccountId(accountId)
                        .build();
        GetIdPMetadataForWorkloadSSOResponse response = newStub(requestId).getIdPMetadataForWorkloadSSO(request);
        return response.getMetadata();
    }

    public UserManagementProto.ListRolesResponse listRoles(String requestId, String accountId) {
        checkNotNull(accountId);
        return newStub(requestId).listRoles(UserManagementProto.ListRolesRequest.newBuilder()
                .setAccountId(accountId)
                .build());
    }

    public UserManagementProto.SetWorkloadAdministrationGroupNameResponse setWorkloadAdministrationGroupName(String requestId, String accountId,
            String right, String resource) {
        checkNotNull(accountId);
        checkNotNull(right);
        checkNotNull(resource);
        return newStub(requestId).setWorkloadAdministrationGroupName(UserManagementProto.SetWorkloadAdministrationGroupNameRequest.newBuilder()
                .setAccountId(accountId)
                .setRightName(right)
                .setResource(resource)
                .build());
    }

    public UserManagementProto.GetWorkloadAdministrationGroupNameResponse getWorkloadAdministrationGroupName(String requestId, String accountId,
            String right, String resource) {
        checkNotNull(accountId);
        checkNotNull(right);
        checkNotNull(resource);
        return newStub(requestId).getWorkloadAdministrationGroupName(UserManagementProto.GetWorkloadAdministrationGroupNameRequest.newBuilder()
                .setAccountId(accountId)
                .setRightName(right)
                .setResource(resource)
                .build());
    }

    public UserManagementProto.DeleteWorkloadAdministrationGroupNameResponse deleteWorkloadAdministrationGroupName(String requestId, String accountId,
            String right, String resource) {
        checkNotNull(accountId);
        checkNotNull(right);
        checkNotNull(resource);
        return newStub(requestId).deleteWorkloadAdministrationGroupName(UserManagementProto.DeleteWorkloadAdministrationGroupNameRequest.newBuilder()
                .setAccountId(accountId)
                .setRightName(right)
                .setResource(resource)
                .build());
    }

    /**
     * Wraps calls to ListWorkloadAdministrationGroups with an Account ID.
     *
     * @param requestId          the request ID for the request
     * @param accountId          the account ID
     * @return the list of workload administration groups
     */
    public List<WorkloadAdministrationGroup> listWorkloadAdministrationGroups(String requestId, String accountId) {
        checkNotNull(requestId);
        checkNotNull(accountId);

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

    private UserManagementProto.Assignee getAssignee(String userCrn) {
        UserManagementProto.Assignee.Builder assignee = UserManagementProto.Assignee.newBuilder()
                .setAccountId(Crn.fromString(userCrn).getAccountId());
        if (Crn.isCrn(userCrn) && Crn.ResourceType.MACHINE_USER.equals(Crn.fromString(userCrn).getResourceType())) {
            assignee.setMachineUserNameOrCrn(userCrn);
        } else {
            assignee.setUserIdOrCrn(userCrn);
        }
        return assignee.build();
    }
}

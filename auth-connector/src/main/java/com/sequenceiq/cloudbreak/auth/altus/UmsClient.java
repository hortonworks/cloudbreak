package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc;
import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc.UserManagementBlockingStub;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsAuthenticationException;

import io.grpc.ManagedChannel;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class UmsClient {

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
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @return the list of users
     */
    public List<User> listUsers(String requestId, String accountId) {
        checkNotNull(requestId);
        checkNotNull(accountId);

        List<User> users = new ArrayList<>();

        ListUsersRequest.Builder requestBuilder = ListUsersRequest.newBuilder()
                .setAccountId(accountId)
                .setPageSize(umsClientConfig.getListUsersPageSize());

        ListUsersResponse response;
        do {
            response = newStub(requestId).listUsers(requestBuilder.build());
            users.addAll(response.getUserList());
            requestBuilder.setPageToken(response.getNextPageToken());
        } while (response.hasNextPageToken());
        return users;
    }

    public MachineUser getMachineUser(String requestId, String userCrn) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        Crn crn = Crn.fromString(userCrn);
        List<MachineUser> machineUsers = newStub(requestId).listMachineUsers(
                ListMachineUsersRequest.newBuilder()
                        .setAccountId(Crn.fromString(userCrn).getAccountId())
                        .addMachineUserNameOrCrn(userCrn)
                        .build()
        ).getMachineUserList();
        checkSingleUserResponse(machineUsers, crn.getResource());
        return machineUsers.get(0);
    }

    /**
     * Wraps calls to ListMachineUsers with an Account ID.
     *
     * @param requestId the request ID for the request
     * @param accountId the account ID
     * @return the list of machine users
     */
    public List<MachineUser> listMachineUsers(String requestId, String accountId) {
        checkNotNull(requestId);
        checkNotNull(accountId);

        List<MachineUser> machineUsers = new ArrayList<>();

        ListMachineUsersRequest.Builder requestBuilder = ListMachineUsersRequest.newBuilder()
                .setAccountId(accountId)
                .setPageSize(umsClientConfig.getListMachineUsersPageSize());

        ListMachineUsersResponse response;
        do {
            response = newStub(requestId).listMachineUsers(requestBuilder.build());
            machineUsers.addAll(response.getMachineUserList());
            requestBuilder.setPageToken(response.getNextPageToken());
        } while (response.hasNextPageToken());
        return machineUsers;
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
                .setAssignee(UserManagementProto.Assignee.newBuilder()
                        .setAccountId(Crn.fromString(userCrn).getAccountId())
                        .setUserIdOrCrn(userCrn)
                        .build())
                .setResourceCrn(resourceCrn)
                .setResourceRoleCrn(resourceRoleCrn)
                .build());
    }

    public void unassignResourceRole(String requestId, String userCrn, String resourceCrn, String resourceRoleCrn) {
        newStub(requestId).unassignResourceRole(UserManagementProto.UnassignResourceRoleRequest.newBuilder()
                .setAssignee(UserManagementProto.Assignee.newBuilder()
                        .setAccountId(Crn.fromString(userCrn).getAccountId())
                        .setUserIdOrCrn(userCrn)
                        .build())
                .setResourceCrn(resourceCrn)
                .setResourceRoleCrn(resourceRoleCrn)
                .build());
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
     * @param userCrn   the user CRN
     * @return the account
     */
    public Account getAccount(String requestId, String userCrn) {
        checkNotNull(requestId);
        checkNotNull(userCrn);
        return newStub(requestId).getAccount(
                GetAccountRequest.newBuilder()
                        .setAccountId(Crn.fromString(userCrn).getAccountId())
                        .build()
        ).getAccount();
    }

    public List<String> getGroupsForUser(String requestId, String actorCrn, String resourceCrn) {
        if (resourceCrn == null) {
            resourceCrn = "*";
        }
        return newStub(requestId).getRights(
                GetRightsRequest.newBuilder()
                        .setActorCrn(actorCrn)
                        .setResourceCrn(resourceCrn)
                        .build()
        ).getGroupCrnList();
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
}

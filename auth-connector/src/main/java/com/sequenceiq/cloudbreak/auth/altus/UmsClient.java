package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc;
import com.cloudera.thunderhead.service.usermanagement.UserManagementGrpc.UserManagementBlockingStub;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListMachineUsersRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListUsersRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsAuthenticationException;

import io.grpc.ManagedChannel;

/**
 * A simple wrapper to the GRPC user management service. This handles setting up
 * the appropriate context-propogatinng interceptors and hides some boilerplate.
 */
public class UmsClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    /**
     * Constructor.
     *
     * @param channel  the managed channel.
     * @param actorCrn the actor CRN.
     */
    UmsClient(ManagedChannel channel,
            String actorCrn) {
        this.channel = checkNotNull(channel);
        this.actorCrn = checkNotNull(actorCrn);
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

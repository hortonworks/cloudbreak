package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto.RightCheck;
import com.cloudera.thunderhead.service.common.paging.PagingProto.PageToken;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.AccessKeyType;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetUserSyncStateModelResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListServicePrincipalCloudIdentitiesResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.RightsCheck;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ServicePrincipalCloudIdentities;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadAdministrationGroup;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentracing.Tracer;

@Component
public class GrpcUmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    private static final Predicate<String> VALID_AUTHZ_RESOURCE = resource -> Crn.isCrn(resource) ||
            StringUtils.isEmpty(resource) || StringUtils.equals(resource, "*");

    private static final int CHECK_RIGHT_HAS_RIGHTS_TRESHOLD = 3;

    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private UmsClientConfig umsClientConfig;

    @Inject
    private Tracer tracer;

    public static GrpcUmsClient createClient(ManagedChannelWrapper channelWrapper, UmsClientConfig clientConfig, Tracer tracer) {
        GrpcUmsClient client = new GrpcUmsClient();
        client.channelWrapper = Preconditions.checkNotNull(channelWrapper, "channelWrapper should not be null.");
        client.umsClientConfig = Preconditions.checkNotNull(clientConfig, "clientConfig should not be null.");
        client.tracer = Preconditions.checkNotNull(tracer, "tracer should not be null.");
        return client;
    }

    /**
     * Create new user group if it does not exist.
     *
     * @param accountId          the account ID
     * @param groupName          the newly created group name
     * @param requestId          an optional request Id
     * @return                   the new or existing user group.
     */
    public Group createGroup(String accountId, String groupName, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Creating new user group '{}', for account '{}' using request ID '{}'...", groupName, accountId, requestId);
        Group newGroup = client.createGroup(RequestIdUtil.getOrGenerate(requestId), accountId, groupName);
        LOGGER.debug("New user group '{}' has been created for account '{}'.", groupName, accountId);
        return newGroup;
    }

    /**
     * Delete user group if it exist.
     *
     * @param accountId          the account ID
     * @param groupName          the newly created group name
     * @param requestId          an optional request Id
     */
    public void deleteGroup(String accountId, String groupName, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Deleting user group '{}', from account '{}' using request ID '{}'...", groupName, accountId, requestId);
        client.deleteGroup(RequestIdUtil.getOrGenerate(requestId), accountId, groupName);
        LOGGER.debug("User group '{}' has been deleted from account '{}'.", groupName, accountId);
    }

    /**
     * Add member to the selected user group if it exist.
     *
     * @param accountId          the account ID
     * @param groupName          the group where user is going to be assigned
     * @param memberCrn          member (e.g., user) CRN
     * @param requestId          an optional request Id
     */
    public void addMemberToGroup(String accountId, String groupName, String memberCrn, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Assigning user '{}' to '{}' group, at account '{}' using request ID '{}'...", memberCrn, groupName, accountId, requestId);
        client.addMemberToGroup(RequestIdUtil.getOrGenerate(requestId), accountId, groupName, memberCrn);
        LOGGER.debug("User '{}' has been added to '{}' group successfully.", groupName, accountId);
    }

    /**
     * Remove member from the selected user group if it is exist.
     *
     * @param accountId          the account ID
     * @param groupName          the group where user is going to be assigned
     * @param memberCrn          member (e.g., user) CRN
     * @param requestId          an optional request Id
     */
    public void removeMemberFromGroup(String accountId, String groupName, String memberCrn, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Removing user '{}' from '{}' group, at account '{}' using request ID '{}'...", memberCrn, groupName, accountId, requestId);
        client.removeMemberFromGroup(RequestIdUtil.getOrGenerate(requestId), accountId, groupName, memberCrn);
        LOGGER.debug("User '{}' has been removed from '{}' group successfully.", groupName, accountId);
    }

    /**
     * List members from the selected user group if it is exist.
     *
     * @param requestId          the request ID for the request
     * @param accountId          the account ID
     * @param groupName          the group where user is going to be assigned
     * @return                   list of user group member CRNs or NULL if the user group does not exist.
     */
    public List<String> listMembersFromGroup(String accountId, String groupName, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Listing members from '{}' group, at account '{}' using request ID '{}'...", groupName, accountId, requestId);
        List<String> members = client.listMembersFromGroup(RequestIdUtil.getOrGenerate(requestId), accountId, groupName);
        LOGGER.debug("User group '{}' contains [{}] members at account '{}'.", groupName, members, accountId);
        return members;
    }

    /**
     * Retrieves list of all groups from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @return the list of groups associated with this account
     */
    public List<Group> listAllGroups(String accountId, Optional<String> requestId) {
        return listGroups(accountId, null, requestId);
    }

    /**
     * Retrieves group list from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @param groupCrns the groups to list. if null or empty then all groups will be listed
     * @return the list of groups associated with this account
     */
    public List<Group> listGroups(String accountId, List<String> groupCrns, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Listing group information for account {} using request ID {}", accountId, requestId);
        List<Group> groups = client.listGroups(RequestIdUtil.getOrGenerate(requestId), accountId, groupCrns);
        LOGGER.debug("{} Groups found for account {}", groups.size(), accountId);
        return groups;
    }

    /**
     * Retrieves group list for a specified member from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @param memberCrn the member to list
     * @return the list of group crns associated with this member
     */
    public List<String> listGroupsForMember(String accountId, String memberCrn, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Listing group information for member {} in account {} using request ID {}", memberCrn, accountId, requestId);
        List<String> groups = client.listGroupsForMembers(RequestIdUtil.getOrGenerate(requestId), accountId, memberCrn);
        LOGGER.debug("{} Groups found for member {} in account {}", groups.size(), memberCrn, accountId);
        return groups;
    }

    /**
     * Retrieves user details from UMS.
     *
     * @param userCrn   the CRN of the user
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    @Cacheable(cacheNames = "umsUserCache", key = "{ #userCrn }")
    public User getUserDetails(String userCrn, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Getting user information for {} using request ID {}", userCrn, requestId);
        User user = client.getUser(RequestIdUtil.getOrGenerate(requestId), userCrn);
        LOGGER.debug("User information retrieved for userCrn: {}", user.getCrn());
        return user;
    }

    /**
     * Set user Workload password.
     *
     * @param userCrn   the CRN of the user
     * @param requestId an optional request Id
     * @return the workload credentials associated with this user CRN
     */
    public UserManagementProto.SetActorWorkloadCredentialsResponse setActorWorkloadPassword(String userCrn, String password,
            Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        String workloadUserName = getUserDetails(userCrn, requestId).getWorkloadUsername();
        LOGGER.debug("Setting workload password for user {} with workload name {}", userCrn, workloadUserName);
        UserManagementProto.SetActorWorkloadCredentialsResponse response = client.setActorWorkloadPassword(RequestIdUtil.getOrGenerate(requestId), userCrn,
                password);
        LOGGER.debug("Workload password has been set for user {} with workload name {}", userCrn, workloadUserName);
        return response;
    }

    /**
     * Retrieves user Workload Credentials.
     *
     * @param userCrn   the CRN of the user
     * @param requestId an optional request Id
     * @return the workload credentials associated with this user CRN
     */
    public UserManagementProto.GetActorWorkloadCredentialsResponse getActorWorkloadCredentials(String userCrn, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Getting workload credentials for user {}", userCrn);
        UserManagementProto.GetActorWorkloadCredentialsResponse response = client.getActorWorkloadCredentials(RequestIdUtil.getOrGenerate(requestId), userCrn);
        LOGGER.debug("Got workload credentials for user {}", userCrn);
        return response;
    }

    /**
     * Retrieves list of all users from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @return the list of users associated with this account
     */
    public List<User> listAllUsers(String accountId, Optional<String> requestId) {
        return listUsers(accountId, null, requestId);
    }

    /**
     * Retrieves user list from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @param userCrns  the users to list. if null or empty then all users will be listed
     * @return the list of users associated with this account
     */
    public List<User> listUsers(String accountId, List<String> userCrns, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Listing user information for account {} using request ID {}", accountId, requestId);
        List<User> users = client.listUsers(RequestIdUtil.getOrGenerate(requestId), accountId, userCrns);
        LOGGER.debug("{} Users found for account {}", users.size(), accountId);
        return users;
    }

    /**
     * Retrieves machine user details from UMS.
     *
     * @param userCrn   the CRN of the user
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    @Cacheable(cacheNames = "umsMachineUserCache", key = "{ #userCrn, #accountId }")
    public MachineUser getMachineUserDetails(String userCrn, String accountId, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Getting machine user information for {} using request ID {}", userCrn, requestId);
        MachineUser machineUser = client.getMachineUser(RequestIdUtil.getOrGenerate(requestId), userCrn, accountId);
        LOGGER.debug("MachineUser information retrieved for userCrn: {}", machineUser.getCrn());
        return machineUser;
    }

    /**
     * Retrieves list of all machine users from UMS.
     *
     * @param accountId                   the account Id
     * @param includeInternal             whether to include internal machine users
     * @param includeWorkloadMachineUsers whether to include workload machine users
     * @param requestId                   an optional request Id
     * @return the user associated with this user CRN
     */
    public List<MachineUser> listAllMachineUsers(String accountId, boolean includeInternal, boolean includeWorkloadMachineUsers,
            Optional<String> requestId) {
        return listMachineUsers(accountId, null,
                includeInternal, includeWorkloadMachineUsers,
                requestId);
    }

    /**
     * Retrieves machine user list from UMS.
     *
     * @param accountId                   the account Id
     * @param machineUserCrns             machine users to list. if null or empty then all machine users will be listed
     * @param includeInternal             whether to include internal machine users
     * @param includeWorkloadMachineUsers whether to include workload machine users
     * @param requestId                   an optional request Id
     * @return the user associated with this user CRN
     */
    public List<MachineUser> listMachineUsers(String accountId, List<String> machineUserCrns,
            boolean includeInternal, boolean includeWorkloadMachineUsers, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Listing machine user information for account {} using request ID {}", accountId, requestId);
        List<MachineUser> machineUsers = client.listMachineUsers(RequestIdUtil.getOrGenerate(requestId),
                accountId, machineUserCrns, includeInternal, includeWorkloadMachineUsers);
        LOGGER.debug("{} Machine users found for account {}", machineUsers.size(), accountId);
        return machineUsers;
    }

    /**
     * Creates new machine user, it queries against the machine user if it has already exist
     *
     * @param machineUserName new machine user name
     * @param userCrn         the CRN of the user
     * @param requestId       an optional request Id
     * @return the machine user crn
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public Optional<String> createMachineUser(String machineUserName, String userCrn, String accountId, Optional<String> requestId) {
        try {
            UmsClient client = makeClient(channelWrapper.getChannel());
            String generatedRequestId = RequestIdUtil.getOrGenerate(requestId);
            LOGGER.debug("Creating machine user {} for {} using request ID {}", machineUserName, userCrn, generatedRequestId);
            Optional<String> machineUserCrn = client.createMachineUser(generatedRequestId, userCrn, accountId, machineUserName);
            if (machineUserCrn.isEmpty()) {
                MachineUser machineUser = client.getMachineUserForUser(RequestIdUtil.getOrGenerate(requestId), userCrn, accountId, machineUserName, true, true);
                machineUserCrn = Optional.of(machineUser.getCrn());
            }
            LOGGER.debug("Machine User information retrieved for userCrn: {}", machineUserCrn.orElse(null));
            return machineUserCrn;
        } catch (StatusRuntimeException ex) {
            if (Status.NOT_FOUND.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Machine user with name %s is not found yet", machineUserName);
                LOGGER.debug(errMessage, ex);
                throw new UmsOperationException(errMessage, ex);
            } else if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot create machinue user '%s' for '%s' as " +
                        "UMS API is UNAVAILABLE at the moment", machineUserName, userCrn);
                LOGGER.debug(errMessage, ex);
                throw new UmsOperationException(errMessage, ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Get or Create new machine user for given machineUserName.
     *
     * @param machineUserName new machine user name
     * @param accountId       the accountId
     * @param requestId       an optional request Id
     * @return the machineUser
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public MachineUser getOrCreateMachineUserWithoutAccessKey(String machineUserName, String accountId, Optional<String> requestId) {
        try {
            UmsClient client = makeClient(channelWrapper.getChannel());
            String generatedRequestId = RequestIdUtil.getOrGenerate(requestId);
            LOGGER.debug("Creating machine user {} for accountId {} using request ID {}", machineUserName, accountId, generatedRequestId);
            MachineUser machineUser = client.getOrCreateMachineUserWithoutAccessKey(generatedRequestId, accountId, machineUserName);
            LOGGER.debug("Machine User retrieved for machineUserName: {}, machineUser: {}", machineUserName, machineUser);
            return machineUser;
        } catch (StatusRuntimeException ex) {
            if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot create machinue user '%s' for '%s' as " +
                        "UMS API is UNAVAILABLE at the moment", machineUserName, accountId);
                LOGGER.debug(errMessage, ex);
                throw new UmsOperationException(errMessage, ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Delete machine user
     *
     * @param machineUserName user name that should be deleted
     * @param userCrn         actor
     * @param requestId       request id for deleting machine user
     */
    public void deleteMachineUser(String machineUserName, String userCrn, String accountId, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        String generatedRequestId = RequestIdUtil.getOrGenerate(requestId);
        LOGGER.debug("Deleting machine user {} by {} using request ID {} (for accountId: {})",
                machineUserName, userCrn, generatedRequestId, accountId);
        client.deleteMachineUser(generatedRequestId, userCrn, accountId, machineUserName);
    }

    /**
     * Retrieves list of service principal cloud identities for an environment from UMS.
     *
     * @param accountId      the account Id
     * @param environmentCrn the environment crn
     * @param requestId      an optional request Id
     * @return list of service principal cloud identities for an environment
     */
    public List<ServicePrincipalCloudIdentities> listServicePrincipalCloudIdentities(String accountId, String environmentCrn, Optional<String> requestId) {
        List<ServicePrincipalCloudIdentities> spCloudIds = new ArrayList<>();
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Listing service principal cloud identities for account {} using request ID {}", accountId, requestId);
        ListServicePrincipalCloudIdentitiesResponse response;
        Optional<PageToken> pageToken = Optional.empty();
        do {
            response = client.listServicePrincipalCloudIdentities(RequestIdUtil.getOrGenerate(requestId), accountId, environmentCrn, pageToken);
            spCloudIds.addAll(response.getServicePrincipalCloudIdentitiesList());
            pageToken = Optional.ofNullable(response.getNextPageToken());
        } while (response.hasNextPageToken());
        LOGGER.debug("{} ServicePrincipalCloudIdentities found for account {}", spCloudIds.size(), accountId);
        return spCloudIds;
    }

    /**
     * Lists the workload administration groups a member belongs to.
     *
     * @param memberCrn the CRN of the user or machine user
     * @param requestId request id for getting rights
     * @return the workload administration groups associated with this user or machine user
     */
    public List<String> listWorkloadAdministrationGroupsForMember(String memberCrn, Optional<String> requestId) {
        requireNonNull(memberCrn);
        List<String> wags = new ArrayList<>();
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Getting workload administration groups for member {}", memberCrn);
        ListWorkloadAdministrationGroupsForMemberResponse response;
        Optional<PageToken> pageToken = Optional.empty();
        do {
            response = client.listWorkloadAdministrationGroupsForMember(RequestIdUtil.getOrGenerate(requestId), memberCrn, pageToken);
            wags.addAll(response.getWorkloadAdministrationGroupNameList());
            pageToken = Optional.ofNullable(response.getNextPageToken());
        } while (response.hasNextPageToken());
        LOGGER.debug("{} workload administration groups found for member {}", wags.size(), memberCrn);
        return wags;
    }

    /**
     * Retrieves account details from UMS, which includes the CM license.
     *
     * @param accountId the account ID
     * @param requestId an optional request Id
     * @return the account associated with this user CRN
     */
    @Cacheable(cacheNames = "umsAccountCache", key = "{ #accountId }")
    public Account getAccountDetails(String accountId, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Getting information for account ID {} using request ID {}", accountId, requestId);
        return client.getAccount(RequestIdUtil.getOrGenerate(requestId), accountId);
    }

    @Cacheable(cacheNames = "umsUserHasRightsForResourceCache", key = "{ #userCrn, #right, #resource }")
    public boolean checkResourceRight(String userCrn, String right, String resource, Optional<String> requestId) {
        if (InternalCrnBuilder.isInternalCrn(userCrn)) {
            LOGGER.info("InternalCrn, allow right {} for user {}!", right, userCrn);
            return true;
        }
        return makeCheckRightCall(userCrn, right, resource, requestId);
    }

    @Cacheable(cacheNames = "umsUserHasRightsForResourceCache", key = "{ #userCrn, #right, #resource }")
    public boolean checkResourceRightLegacy(String userCrn, String right, String resource, Optional<String> requestId) {
        if (InternalCrnBuilder.isInternalCrn(userCrn)) {
            LOGGER.info("InternalCrn, allow right {} for user {}!", right, userCrn);
            return true;
        }
        if (isReadRight(right)) {
            LOGGER.info("In account {} authorization related entitlement disabled, thus skipping permission check!!",
                    ThreadBasedUserCrnProvider.getAccountId());
            return true;
        }
        return makeCheckRightCall(userCrn, right, resource, requestId);
    }

    @Cacheable(cacheNames = "umsUserRightsCache", key = "{ #userCrn, #right }")
    public boolean checkAccountRight(String userCrn, String right, Optional<String> requestId) {
        if (InternalCrnBuilder.isInternalCrn(userCrn)) {
            LOGGER.info("InternalCrn, allow account right {} for user {}!", right, userCrn);
            return true;
        }
        return makeCheckRightCall(userCrn, right, null, requestId);
    }

    private boolean makeCheckRightCall(String userCrn, String right, String resource, Optional<String> requestId) {
        checkArgument(VALID_AUTHZ_RESOURCE.test(resource), String.format("Provided resource [%s] is not in CRN format", resource));
        try {
            AuthorizationClient client = makeAuthorizationClient();
            LOGGER.info("Checking right {} for user {} on resource {}!", right, userCrn, resource != null ? resource : "account");
            client.checkRight(RequestIdUtil.getOrGenerate(requestId), userCrn, right, resource);
            LOGGER.info("User {} has right {} on resource {}!", userCrn, right, resource != null ? resource : "account");
            return true;
        } catch (StatusRuntimeException statusRuntimeException) {
            if (Status.Code.PERMISSION_DENIED.equals(statusRuntimeException.getStatus().getCode())) {
                LOGGER.error("Checking right {} failed for user {}, thus access is denied! Cause: {}", right, userCrn, statusRuntimeException.getMessage());
                return false;
            } else if (Status.Code.DEADLINE_EXCEEDED.equals(statusRuntimeException.getStatus().getCode())) {
                LOGGER.error("Deadline exceeded for check right {} for user {} on resource {}", right, userCrn, resource != null ? resource : "account",
                        statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call timed out.");
            } else {
                LOGGER.error("Status runtime exception while checking right {} for user {} on resource {}", right, userCrn, resource != null ? resource :
                        "account", statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call failed.");
            }
        } catch (Exception e) {
            LOGGER.error("Unkown error while checking right {} for user {} on resource {}", right, userCrn, resource != null ? resource :
                    "account", e);
            throw new CloudbreakServiceException("Authorization failed due to user management service call failed with error.");
        }
    }

    /**
     * Retrieves whether the member has the specified rights.
     *
     * @param memberCrn   the CRN of the member
     * @param rightChecks the rights to check
     * @param requestId   an optional request id
     * @return a list of booleans indicating whether the member has the specified rights
     */
    @Cacheable(cacheNames = "umsUserHasRightsCache", key = "{ #memberCrn, #rightChecks }")
    public List<Boolean> hasRights(String memberCrn, List<RightCheck> rightChecks, Optional<String> requestId) {
        return hasRightsNoCache(memberCrn, rightChecks, requestId);
    }

    /**
     * Retrieves whether the member has the specified rights. This method specifically does not cache
     * results for cases where caching affects application correctness.
     *
     * @param memberCrn   the CRN of the member
     * @param rightChecks the rights to check
     * @param requestId   an optional request id
     * @return a list of booleans indicating whether the member has the specified rights
     */
    public List<Boolean> hasRightsNoCache(String memberCrn, List<RightCheck> rightChecks, Optional<String> requestId) {
        LOGGER.info("Checking whether member [{}] has rights [{}]", memberCrn,
                rightChecks.stream().map(this::rightCheckToString).collect(Collectors.toList()));
        checkArgument(rightChecks.stream().map(RightCheck::getResource).allMatch(VALID_AUTHZ_RESOURCE),
                String.format("Following resources are not provided in CRN format: %s.", Joiner.on(",").join(
                        rightChecks.stream().map(RightCheck::getResource).filter(Predicate.not(VALID_AUTHZ_RESOURCE)).collect(Collectors.toList()))));
        if (InternalCrnBuilder.isInternalCrn(memberCrn)) {
            LOGGER.info("InternalCrn has all rights");
            return rightChecks.stream().map(rightCheck -> Boolean.TRUE).collect(Collectors.toList());
        }
        if (!rightChecks.isEmpty()) {
            List<Boolean> retVal;
            if (rightChecks.size() < CHECK_RIGHT_HAS_RIGHTS_TRESHOLD) {
                retVal = rightChecks.stream()
                        .map(check -> makeCheckRightCall(memberCrn, check.getRight(), check.getResource(), requestId))
                        .collect(Collectors.toList());
            } else {
                AuthorizationClient client = makeAuthorizationClient();
                retVal = client.hasRights(RequestIdUtil.getOrGenerate(requestId), memberCrn, rightChecks);
            }
            LOGGER.info("member {} has rights {}", memberCrn, retVal);
            return retVal;
        }
        return List.of();
    }

    public String rightCheckToString(RightCheck rightCheck) {
        String right = rightCheck.getRight();
        String resource = "account";
        if (StringUtils.isNotBlank(rightCheck.getResource())) {
            resource = rightCheck.getResource();
        }
        return Joiner.on(" ").join(Lists.newArrayList(right, "for", resource));
    }

    @Cacheable(cacheNames = "umsUserHasRightsForResourceCache", key = "{ #memberCrn, #resources, #right }")
    public Map<String, Boolean> hasRights(String memberCrn, List<String> resources, String right, Optional<String> requestId) {
        List<RightCheck> rightChecks = resources.stream()
                .map(resource -> RightCheck.newBuilder()
                        .setResource(resource)
                        .setRight(right)
                        .build())
                .collect(Collectors.toList());
        LOGGER.debug("Check if {} has rights to resources {}", memberCrn, rightChecks);
        List<Boolean> result = hasRights(memberCrn, rightChecks, requestId);
        return resources.stream().collect(
                Collectors.toMap(resource -> resource, resource -> result.get(resources.indexOf(resource))));
    }

    public List<Boolean> hasRightsOnResources(String memberCrn, List<String> resourceCrns, String right, Optional<String> requestId) {
        checkArgument(resourceCrns.stream().allMatch(VALID_AUTHZ_RESOURCE),
                String.format("Following resources are not provided in CRN format: %s.", Joiner.on(",").join(
                        resourceCrns.stream().filter(Predicate.not(VALID_AUTHZ_RESOURCE)).collect(Collectors.toList()))));
        if (CollectionUtils.isEmpty(resourceCrns)) {
            return List.of();
        }
        if (InternalCrnBuilder.isInternalCrn(memberCrn)) {
            return resourceCrns.stream().map(r -> true).collect(Collectors.toList());
        }
        LOGGER.debug("Check if {} has rights on resources {}", memberCrn, resourceCrns);
        PersonalResourceViewClient client = new PersonalResourceViewClient(channelWrapper.getChannel(), memberCrn, umsClientConfig, tracer);
        List<Boolean> retVal = client.hasRightOnResources(RequestIdUtil.getOrGenerate(requestId), memberCrn, right, resourceCrns);
        LOGGER.info("member {} has rights {}", memberCrn, retVal);
        return retVal;
    }

    /**
     * Add role to machine user
     *
     * @param userCrn        actor that will assign the role
     * @param machineUserCrn machine user
     * @param roleCrn        role that will be assigned
     * @param requestId      id for the request
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public void assignMachineUserRole(String userCrn, String accountId, String machineUserCrn, String roleCrn, Optional<String> requestId) {
        try {
            UmsClient client = makeClient(channelWrapper.getChannel());
            client.assignMachineUserRole(RequestIdUtil.getOrGenerate(requestId), userCrn, accountId, machineUserCrn, roleCrn);
        } catch (StatusRuntimeException ex) {
            if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot assign role '%s' to machine user '%s' as " +
                        "UMS API is UNAVAILABLE at the moment", machineUserCrn, roleCrn);
                LOGGER.debug(errMessage, ex);
                throw new UmsOperationException(errMessage, ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Remove machine user role
     *
     * @param machineUserCrn machine user
     * @param roleCrn        role that will be removed
     * @param requestId      id for the request
     */
    public void unassignMachineUserRole(String machineUserCrn,
            String roleCrn, Optional<String> requestId, String accountId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        client.unassignMachineUserRole(RequestIdUtil.getOrGenerate(requestId), machineUserCrn, roleCrn, accountId);
    }

    /**
     * Generate access / private keypair
     *
     * @param actorCrn       actor that executes the key generation
     * @param machineUserCrn machine user (owner of the access key)
     * @param requestId      id for the request
     * @param accessKeyType  algorithm type used for the access key
     * @return access / private key holder object
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public AltusCredential generateAccessSecretKeyPair(String actorCrn, String accountId, String machineUserCrn,
            Optional<String> requestId, AccessKeyType.Value accessKeyType) {
        try {
            UmsClient client = makeClient(channelWrapper.getChannel());
            LOGGER.info("Generating new access / secret key pair for {}", machineUserCrn);
            CreateAccessKeyResponse accessKeyResponse = client.createAccessPrivateKeyPair(
                    RequestIdUtil.getOrGenerate(requestId), actorCrn, accountId, machineUserCrn, accessKeyType);
            return new AltusCredential(accessKeyResponse.getAccessKey().getAccessKeyId(), accessKeyResponse.getPrivateKey().toCharArray());
        } catch (StatusRuntimeException ex) {
            if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot generate access key pair for machine user '%s' as " +
                        "UMS API is UNAVAILABLE at the moment", machineUserCrn);
                LOGGER.debug(errMessage, ex);
                throw new UmsOperationException(errMessage, ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Generate new machine user (if it is needed) and access api key for this user.
     * Also assign built-in dabaus uploader role for the machine user (if the role is not empty).
     *
     * @param machineUserName machine user name
     * @param userCrn         crn of the actor
     * @param roleCrn         crn of the role
     * @return credential (access/secret keypair)
     */
    public AltusCredential createMachineUserAndGenerateKeys(String machineUserName, String userCrn, String accountId, String roleCrn) {
        return createMachineUserAndGenerateKeys(machineUserName, userCrn, accountId, roleCrn, AccessKeyType.Value.UNSET);
    }

    /**
     * Generate new machine user (if it is needed) and access api key for this user.
     * Also assign built-in dabaus uploader role for the machine user (if the role is not empty).
     *
     * @param machineUserName machine user name
     * @param userCrn         crn of the actor
     * @param roleCrn         crn of the role
     * @param accessKeyType   algorithm type used for the access key
     * @return credential (access/secret keypair)
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public AltusCredential createMachineUserAndGenerateKeys(String machineUserName, String userCrn, String accountId,
            String roleCrn, AccessKeyType.Value accessKeyType) {
        Optional<String> machineUserCrn = createMachineUser(machineUserName, userCrn, accountId, MDCUtils.getRequestId());
        if (StringUtils.isNotEmpty(roleCrn)) {
            assignMachineUserRole(userCrn, accountId, machineUserCrn.orElse(null), roleCrn, MDCUtils.getRequestId());
        }
        return generateAccessSecretKeyPair(userCrn, accountId, machineUserCrn.orElse(null), MDCUtils.getRequestId(), accessKeyType);
    }

    /**
     * Cleanup machine user related resources (access keys, role, user)
     *
     * @param machineUserName machine user name
     * @param userCrn         crn of the actor
     * @param roleCrn         crn of the role
     */
    public void clearMachineUserWithAccessKeysAndRole(String machineUserName, String userCrn, String accountId, String roleCrn) {
        if (StringUtils.isNotEmpty(roleCrn)) {
            unassignMachineUserRole(machineUserName, roleCrn, MDCUtils.getRequestId(), accountId);
        }
        deleteMachineUserAccessKeys(userCrn, accountId, machineUserName, MDCUtils.getRequestId());
        deleteMachineUser(machineUserName, userCrn, accountId, MDCUtils.getRequestId());
    }

    /**
     * Delete all access key for machine user
     *
     * @param actorCrn       actor that executes the deletions
     * @param machineUserCrn machine user
     * @param requestId      id for the request
     */
    public void deleteMachineUserAccessKeys(String actorCrn, String accountId, String machineUserCrn, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.info("Getting access keys for {}", machineUserCrn);
        List<String> accessKeys = client.listMachineUserAccessKeys(RequestIdUtil.getOrGenerate(requestId), actorCrn, accountId, machineUserCrn);
        LOGGER.info("Deleting access keys for {}", machineUserCrn);
        client.deleteAccessKeys(MDCBuilder.getOrGenerateRequestId(), accessKeys, accountId);
    }

    /**
     * Check that machine user has a specific access key in UMS
     *
     * @param actorCrn       actor for the machine user request
     * @param accountId      the account ID
     * @param machineUserCrn machine user crn that own the access key
     * @param accessKeyId    access key id that we need to check
     * @return result that is true if the machine user has the queried access key in UMS
     */
    public boolean doesMachineUserHasAccessKey(String actorCrn, String accountId,
            String machineUserCrn, String accessKeyId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        List<String> accessKeys = client.listMachineUserAccessKeys(MDCBuilder.getOrGenerateRequestId(), actorCrn, accountId, machineUserCrn, true);
        return accessKeys.contains(accessKeyId);
    }

    /**
     * Gather anonymization rules for a specific account
     * NOTE: not supported yet on UMS side
     *
     * @param accountId account that owns the anonymization rules
     * @param actorCrn  actor that requests to gather the anonymization rules
     * @return a list of anonymization rules for an UMS account
     */
    public List<AnonymizationRule> getAnonymizationRules(String accountId, String actorCrn) {
        LOGGER.debug("Try getting anonymization rules for {} by {}", accountId, actorCrn);
        return new ArrayList<>();
    }

    @VisibleForTesting
    UmsClient makeClient(ManagedChannel channel) {
        return new UmsClient(channel, umsClientConfig, tracer);
    }

    @VisibleForTesting
    AuthorizationClient makeAuthorizationClient() {
        return new AuthorizationClient(channelWrapper.getChannel(), umsClientConfig, tracer);
    }

    /**
     * Queries the metadata file used to configure SSO authentication on clusters.
     *
     * @param accountId the account ID
     * @return metadata as string
     */
    public String getIdentityProviderMetadataXml(String accountId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        String requestId = MDCBuilder.getOrGenerateRequestId();
        LOGGER.debug("Getting IdP metadata through account ID: {}, request id: {}", accountId, requestId);
        return client.getIdentityProviderMetadataXml(requestId, accountId);
    }

    public Multimap<String, String> listAssignedResourceRoles(String assigneeCrn, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.info("List the assigned resource roles for {} assignee", assigneeCrn);
        UserManagementProto.ListAssignedResourceRolesResponse response =
                client.listAssignedResourceRoles(RequestIdUtil.getOrGenerate(requestId), assigneeCrn);
        Multimap<String, String> resourceRoleAssignments = response.getResourceAssignmentList().stream()
                .collect(Multimaps
                        .toMultimap(
                                UserManagementProto.ResourceAssignment::getResourceCrn,
                                UserManagementProto.ResourceAssignment::getResourceRoleCrn,
                                ArrayListMultimap::create));
        LOGGER.info("Assigned resource roles [{}] for assignee {}", resourceRoleAssignments, assigneeCrn);
        return resourceRoleAssignments;
    }

    public void assignResourceRole(String assigneeCrn, String resourceCrn, String resourceRoleCrn, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.info("Assigning '{}' role at resource '{}' to assignee '{}'...", resourceRoleCrn, resourceCrn, assigneeCrn);
        client.assignResourceRole(RequestIdUtil.getOrGenerate(requestId), assigneeCrn, resourceCrn, resourceRoleCrn);
        LOGGER.info("Resource role assignment has been successfully done ::: Role: {} Resource: {} Assignee: {}",
                resourceRoleCrn, resourceCrn, assigneeCrn);
    }

    public void unassignResourceRole(String assigneeCrn, String resourceCrn, String resourceRoleCrn, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.info("Unassigning '{}' role at resource '{}' from assignee '{}'...", resourceRoleCrn, resourceCrn, assigneeCrn);
        client.unassignResourceRole(RequestIdUtil.getOrGenerate(requestId), assigneeCrn, resourceCrn, resourceRoleCrn);
        LOGGER.info("Resource role unassignment has been successfully done ::: Role: {} Resource: {} Assignee: {}",
                resourceRoleCrn, resourceCrn, assigneeCrn);
    }

    public void notifyResourceDeleted(String resourceCrn, Optional<String> requestId) {
        try {
            LOGGER.debug("Notify UMS about resource ('{}') was deleted", resourceCrn);
            UmsClient client = makeClient(channelWrapper.getChannel());
            client.notifyResourceDeleted(RequestIdUtil.getOrGenerate(requestId), resourceCrn);
            LOGGER.info("Notified UMS about deletion of resource {}", resourceCrn);
        } catch (Exception e) {
            LOGGER.error(String.format("Notifying UMS about deletion of resource %s has failed: ", resourceCrn), e);
        }
    }

    public String setWorkloadAdministrationGroupName(String accountId, Optional<String> requestId, String right, String resource) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        return client.setWorkloadAdministrationGroupName(RequestIdUtil.getOrGenerate(requestId),
                accountId, right, resource).getWorkloadAdministrationGroupName();
    }

    public String getWorkloadAdministrationGroupName(String accountId, Optional<String> requestId, String right, String resource) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        return client.getWorkloadAdministrationGroupName(RequestIdUtil.getOrGenerate(requestId),
                accountId, right, resource).getWorkloadAdministrationGroupName();
    }

    public void deleteWorkloadAdministrationGroupName(String accountId, Optional<String> requestId, String right, String resource) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        client.deleteWorkloadAdministrationGroupName(RequestIdUtil.getOrGenerate(requestId), accountId, right, resource);
    }

    public List<WorkloadAdministrationGroup> listWorkloadAdministrationGroups(String accountId, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        return client.listWorkloadAdministrationGroups(RequestIdUtil.getOrGenerate(requestId), accountId);
    }

    /**
     * Retrieves event generation ids for an account from UMS.
     *
     * @param accountId the account id
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    public GetEventGenerationIdsResponse getEventGenerationIds(String accountId, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        LOGGER.debug("Getting event generation ids for account {} using request ID {}", accountId, requestId);
        return client.getEventGenerationIds(RequestIdUtil.getOrGenerate(requestId), accountId);
    }

    /**
     * Retrieves user sync state model from UMS.
     *
     * @param accountId    the account Id
     * @param requestId    an optional request Id
     * @param rightsChecks list of rights checks for resources. a List is used to preserve order.
     * @return the user sync state for this account and rights checks
     */
    public GetUserSyncStateModelResponse getUserSyncStateModel(String accountId, List<RightsCheck> rightsChecks, Optional<String> requestId) {
        UmsClient client = makeClient(channelWrapper.getChannel());
        String generatedRequestId = RequestIdUtil.getOrGenerate(requestId);
        LOGGER.debug("Retrieving user sync state model for account {} using request ID {}", accountId, generatedRequestId);
        return client.getUserSyncStateModel(generatedRequestId, accountId, rightsChecks);
    }

    protected boolean isReadRight(String action) {
        if (action == null) {
            return false;
        }
        String[] parts = action.split("/");
        if (parts.length == 2 && parts[1] != null && parts[1].equals("read")) {
            return true;
        }
        return false;
    }

    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }
}

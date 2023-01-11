package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Component
public class GrpcUmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    private static final Predicate<String> VALID_AUTHZ_RESOURCE = resource -> Crn.isCrn(resource) ||
            StringUtils.isEmpty(resource) || StringUtils.equals(resource, "*");

    private static final int CHECK_RIGHT_HAS_RIGHTS_TRESHOLD = 3;

    @Qualifier("umsManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private UmsClientConfig umsClientConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public static GrpcUmsClient createClient(ManagedChannelWrapper channelWrapper, UmsClientConfig clientConfig) {
        GrpcUmsClient client = new GrpcUmsClient();
        client.channelWrapper = Preconditions.checkNotNull(channelWrapper, "channelWrapper should not be null.");
        client.umsClientConfig = Preconditions.checkNotNull(clientConfig, "clientConfig should not be null.");
        return client;
    }

    /**
     * Create new user group if it does not exist.
     *
     * @param accountId          the account ID
     * @param groupName          the newly created group name
     * @return                   the new or existing user group.
     */
    public Group createGroup(String accountId, String groupName,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Creating new user group '{}', for account '{}'.", groupName, accountId);
        Group newGroup = client.createGroup(accountId, groupName);
        LOGGER.debug("New user group '{}' has been created for account '{}'.", groupName, accountId);
        return newGroup;
    }

    /**
     * Delete user group if it exist.
     *
     * @param accountId          the account ID
     * @param groupName          the newly created group name
     */
    public void deleteGroup(String accountId, String groupName,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Deleting user group '{}', from account '{}'.", groupName, accountId);
        client.deleteGroup(accountId, groupName);
        LOGGER.debug("User group '{}' has been deleted from account '{}'.", groupName, accountId);
    }

    /**
     * Add member to the selected user group if it exist.
     *
     * @param accountId          the account ID
     * @param groupName          the group where user is going to be assigned
     * @param memberCrn          member (e.g., user) CRN
     */
    public void addMemberToGroup(String accountId, String groupName, String memberCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Assigning user '{}' to '{}' group, at account '{}'.", memberCrn, groupName);
        client.addMemberToGroup(accountId, groupName, memberCrn);
        LOGGER.debug("User '{}' has been added to '{}' group successfully.", groupName, accountId);
    }

    /**
     * Remove member from the selected user group if it is exist.
     *
     * @param accountId          the account ID
     * @param groupName          the group where user is going to be assigned
     * @param memberCrn          member (e.g., user) CRN
     */
    public void removeMemberFromGroup(String accountId, String groupName, String memberCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Removing user '{}' from '{}' group, at account '{}'.", memberCrn, groupName, accountId);
        client.removeMemberFromGroup(accountId, groupName, memberCrn);
        LOGGER.debug("User '{}' has been removed from '{}' group successfully.", groupName, accountId);
    }

    /**
     * List members from the selected user group if it is exist.
     *
     * @param accountId          the account ID
     * @param groupName          the group where user is going to be assigned
     * @return                   list of user group member CRNs or NULL if the user group does not exist.
     */
    public List<String> listMembersFromGroup(String accountId, String groupName,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Listing members from '{}' group, at account '{}'.", groupName, accountId);
        List<String> members = client.listMembersFromGroup(accountId, groupName);
        LOGGER.debug("User group '{}' contains [{}] members at account '{}'.", groupName, members, accountId);
        return members;
    }

    /**
     * Retrieves list of all groups from UMS.
     *
     * @param accountId the account Id
     * @return the list of groups associated with this account
     */
    public List<Group> listAllGroups(String accountId,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return listGroups(accountId, null, regionAwareInternalCrnGeneratorFactory);
    }

    /**
     * Retrieves group list from UMS.
     *
     * @param accountId the account Id
     * @param groupCrns the groups to list. if null or empty then all groups will be listed
     * @return the list of groups associated with this account
     */
    public List<Group> listGroups(String accountId, List<String> groupCrns,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Listing group information for account {}.", accountId);
        List<Group> groups = client.listGroups(accountId, groupCrns);
        LOGGER.debug("{} Groups found for account {}", groups.size(), accountId);
        return groups;
    }

    /**
     * Retrieves group list for a specified member from UMS.
     *
     * @param accountId the account Id
     * @param memberCrn the member to list
     * @return the list of group crns associated with this member
     */
    public List<String> listGroupsForMember(String accountId, String memberCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Listing group information for member {} in account {}.", memberCrn, accountId);
        List<String> groups = client.listGroupsForMembers(accountId, memberCrn);
        LOGGER.debug("{} Groups found for member {} in account {}", groups.size(), memberCrn, accountId);
        return groups;
    }

    /**
     * Retrieves user details from UMS.
     *
     * @param userCrn   the CRN of the user.
     * @return the user associated with this user CRN
     */
    @Cacheable(cacheNames = "umsUserCache", key = "{ #userCrn }")
    public User getUserDetails(String userCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Getting user information for {}.", userCrn);
        User user = client.getUser(userCrn);
        LOGGER.debug("User information retrieved for userCrn: {}", user.getCrn());
        return user;
    }

    /**
     * Set user Workload password.
     *
     * @param userCrn   the CRN of the user.
     * @return the workload credentials associated with this user CRN
     */
    public UserManagementProto.SetActorWorkloadCredentialsResponse setActorWorkloadPassword(String userCrn, String password,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        String workloadUserName = getUserDetails(userCrn, regionAwareInternalCrnGeneratorFactory).getWorkloadUsername();
        LOGGER.debug("Setting workload password for user {} with workload name {}", userCrn, workloadUserName);
        UserManagementProto.SetActorWorkloadCredentialsResponse response = client.setActorWorkloadPassword(userCrn,
                password);
        LOGGER.debug("Workload password has been set for user {} with workload name {}", userCrn, workloadUserName);
        return response;
    }

    /**
     * Retrieves user Workload Credentials.
     *
     * @param userCrn   the CRN of the user.
     * @return the workload credentials associated with this user CRN
     */
    public UserManagementProto.GetActorWorkloadCredentialsResponse getActorWorkloadCredentials(String userCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Getting workload credentials for user {}", userCrn);
        UserManagementProto.GetActorWorkloadCredentialsResponse response = client.getActorWorkloadCredentials(userCrn);
        LOGGER.debug("Got workload credentials for user {}", userCrn);
        return response;
    }

    /**
     * Retrieves list of all users from UMS.
     *
     * @param accountId the account Id.
     * @return the list of users associated with this account
     */
    public List<User> listAllUsers(String accountId,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return listUsers(accountId, null, regionAwareInternalCrnGeneratorFactory);
    }

    /**
     * Retrieves user list from UMS.
     *
     * @param accountId the account Id.
     * @param userCrns  the users to list. if null or empty then all users will be listed
     * @return the list of users associated with this account
     */
    public List<User> listUsers(String accountId, List<String> userCrns,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Listing user information for account {}.", accountId);
        List<User> users = client.listUsers(accountId, userCrns);
        LOGGER.debug("{} Users found for account {}", users.size(), accountId);
        return users;
    }

    /**
     * Retrieves machine user details from UMS.
     *
     * @param userCrn   the CRN of the user.
     * @return the user associated with this user CRN
     */
    @Cacheable(cacheNames = "umsMachineUserCache", key = "{ #userCrn, #accountId }")
    public MachineUser getMachineUserDetails(String userCrn, String accountId,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Getting machine user information for {}.", userCrn);
        MachineUser machineUser = client.getMachineUser(userCrn, accountId);
        LOGGER.debug("MachineUser information retrieved for userCrn: {}", machineUser.getCrn());
        return machineUser;
    }

    /**
     * Set machine user Workload password.
     *
     * @param userCrn   the CRN of the machine user.
     * @return the workload credentials associated with this machine user CRN
     */
    public UserManagementProto.SetActorWorkloadCredentialsResponse setMachineUserWorkloadPassword(String userCrn, String accountId, String password,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        String workloadUserName = getMachineUserDetails(userCrn, accountId, regionAwareInternalCrnGeneratorFactory).getWorkloadUsername();
        LOGGER.debug("Setting workload password for machine user {} with workload name {}", userCrn, workloadUserName);
        UserManagementProto.SetActorWorkloadCredentialsResponse response = client.setActorWorkloadPassword(userCrn,
                password);
        LOGGER.debug("Workload password has been set for machine user {} with workload name {}", userCrn, workloadUserName);
        return response;
    }

    /**
     * Retrieves list of all machine users from UMS.
     *
     * @param accountId                   the account Id
     * @param includeInternal             whether to include internal machine users
     * @param includeWorkloadMachineUsers whether to include workload machine users
     * @return the user associated with this user CRN
     */
    public List<MachineUser> listAllMachineUsers(String accountId, boolean includeInternal, boolean includeWorkloadMachineUsers,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return listMachineUsers(accountId, null,
                includeInternal, includeWorkloadMachineUsers,
                regionAwareInternalCrnGeneratorFactory);
    }

    /**
     * Retrieves machine user list from UMS.
     *
     * @param accountId                   the account Id
     * @param machineUserCrns             machine users to list. if null or empty then all machine users will be listed
     * @param includeInternal             whether to include internal machine users
     * @param includeWorkloadMachineUsers whether to include workload machine users
     * @return the user associated with this user CRN
     */
    public List<MachineUser> listMachineUsers(String accountId, List<String> machineUserCrns,
            boolean includeInternal, boolean includeWorkloadMachineUsers,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Listing machine user information for account {}.", accountId);
        List<MachineUser> machineUsers = client.listMachineUsers(
                accountId, machineUserCrns, includeInternal, includeWorkloadMachineUsers);
        LOGGER.debug("{} Machine users found for account {}", machineUsers.size(), accountId);
        return machineUsers;
    }

    /**
     * Creates new machine user, it queries against the machine user if it has already exist
     *
     * @param machineUserName new machine user name
     * @param userCrn         the CRN of the user
     * @return the machine user crn
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public Optional<String> createMachineUser(String machineUserName, String userCrn, String accountId,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        try {
            UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
            LOGGER.debug("Creating machine user {} for {}.", machineUserName, userCrn);
            Optional<String> machineUserCrn = client.createMachineUser(userCrn, accountId, machineUserName);
            if (machineUserCrn.isEmpty()) {
                MachineUser machineUser = client.getMachineUserForUser(userCrn, accountId, machineUserName, true, true);
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
     * @return the machineUser
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public MachineUser getOrCreateMachineUserWithoutAccessKey(String machineUserName, String accountId) {
        try {
            UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
            LOGGER.debug("Creating machine user {} for accountId {}.", machineUserName, accountId);
            MachineUser machineUser = client.getOrCreateMachineUserWithoutAccessKey(accountId, machineUserName);
            LOGGER.debug("Machine User retrieved for machineUserName: {}, machineUser: {}", machineUserName, machineUser);
            return machineUser;
        } catch (StatusRuntimeException ex) {
            if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot create machine user '%s' for '%s' as " +
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
     */
    public void deleteMachineUser(String machineUserName, String userCrn, String accountId,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Deleting machine user {} by {}. (for accountId: {})",
                machineUserName, userCrn, accountId);
        client.deleteMachineUser(userCrn, accountId, machineUserName);
    }

    /**
     * Retrieves list of service principal cloud identities for an environment from UMS.
     *
     * @param accountId      the account Id
     * @param environmentCrn the environment crn
     * @return list of service principal cloud identities for an environment
     */
    public List<ServicePrincipalCloudIdentities> listServicePrincipalCloudIdentities(String accountId, String environmentCrn) {
        List<ServicePrincipalCloudIdentities> spCloudIds = new ArrayList<>();
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Listing service principal cloud identities for account {}.", accountId);
        ListServicePrincipalCloudIdentitiesResponse response;
        Optional<PageToken> pageToken = Optional.empty();
        do {
            response = client.listServicePrincipalCloudIdentities(accountId, environmentCrn, pageToken);
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
     * @return the workload administration groups associated with this user or machine user
     */
    public List<String> listWorkloadAdministrationGroupsForMember(String memberCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        requireNonNull(memberCrn);
        List<String> wags = new ArrayList<>();
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Getting workload administration groups for member {}", memberCrn);
        ListWorkloadAdministrationGroupsForMemberResponse response;
        Optional<PageToken> pageToken = Optional.empty();
        do {
            response = client.listWorkloadAdministrationGroupsForMember(memberCrn, pageToken);
            wags.addAll(response.getWorkloadAdministrationGroupNameList());
            pageToken = Optional.ofNullable(response.getNextPageToken());
        } while (response.hasNextPageToken());
        LOGGER.debug("{} workload administration groups found for member {}", wags.size(), memberCrn);
        return wags;
    }

    /**
     * Retrieves account details from UMS, which includes the CM license.
     *
     * @param accountId the account ID.
     * @return the account associated with this user CRN
     */
    @Cacheable(cacheNames = "umsAccountCache", key = "{ #accountId }")
    public Account getAccountDetails(String accountId,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Getting information for account ID {}.", accountId);
        return client.getAccount(accountId);
    }

    @Cacheable(cacheNames = "umsUserHasRightsForResourceCache", key = "{ #userCrn, #right, #resource }")
    public boolean checkResourceRight(String userCrn, String right, String resource,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrn)) {
            LOGGER.info("InternalCrn, allow right {} for user {}!", right, userCrn);
            return true;
        }
        return makeCheckRightCall(userCrn, right, resource, regionAwareInternalCrnGeneratorFactory);
    }

    @Cacheable(cacheNames = "umsUserRightsCache", key = "{ #userCrn, #right }")
    public boolean checkAccountRight(String userCrn, String right,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrn)) {
            LOGGER.info("InternalCrn, allow account right {} for user {}!", right, userCrn);
            return true;
        }
        return makeCheckRightCall(userCrn, right, null, regionAwareInternalCrnGeneratorFactory);
    }

    private boolean makeCheckRightCall(String userCrn, String right, String resource,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        checkArgument(VALID_AUTHZ_RESOURCE.test(resource), String.format("Provided resource [%s] is not in CRN format", resource));
        return makeCheckRightCallAndHandleExceptions(userCrn, right, resource, regionAwareInternalCrnGeneratorFactory);
    }

    private boolean makeCheckRightCallAndHandleExceptions(String userCrn, String right, String resource,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        try {
            return checkRight(userCrn, right, resource, regionAwareInternalCrnGeneratorFactory);
        } catch (StatusRuntimeException statusRuntimeException) {
            if (Status.Code.PERMISSION_DENIED.equals(statusRuntimeException.getStatus().getCode())) {
                LOGGER.error("Checking right {} failed for user {}, thus access is denied! Cause: {}", right, userCrn, statusRuntimeException.getMessage());
                return false;
            } else if (Status.Code.DEADLINE_EXCEEDED.equals(statusRuntimeException.getStatus().getCode())) {
                LOGGER.error("Deadline exceeded for check right {} for user {} on resource {}", right, userCrn, resource != null ? resource : "account",
                        statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call timed out.");
            } else if (Status.Code.NOT_FOUND.equals(statusRuntimeException.getStatus().getCode())) {
                LOGGER.error("NOT_FOUND for check right {} for user {} on resource {}, thus access is denied! Cause: {}",
                        right, userCrn, resource, statusRuntimeException.getMessage());
                throw new UnauthorizedException("Authorization failed for user: " + userCrn);
            } else {
                LOGGER.error("Status runtime exception while checking right {} for user {} on resource {}", right, userCrn, resource != null ? resource :
                        "account", statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call failed.");
            }
        } catch (Exception e) {
            LOGGER.error("Unknown error while checking right {} for user {} on resource {}", right, userCrn, resource != null ? resource :
                    "account", e);
            throw new CloudbreakServiceException("Authorization failed due to user management service call failed with error.");
        }
    }

    private boolean checkRight(String userCrn, String right, String resource,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        AuthorizationClient client = makeAuthorizationClient(regionAwareInternalCrnGeneratorFactory);
        LOGGER.info("Checking right {} for user {} on resource {}!", right, userCrn, resource != null ? resource : "account");
        client.checkRight(userCrn, right, resource);
        LOGGER.info("User {} has right {} on resource {}!", userCrn, right, resource != null ? resource : "account");
        return true;
    }

    /**
     * Retrieves whether the member has the specified rights.
     *
     * @param memberCrn   the CRN of the member
     * @param rightChecks the rights to check
     * @return a list of booleans indicating whether the member has the specified rights
     */
    @Cacheable(cacheNames = "umsUserHasRightsCache", key = "{ #memberCrn, #rightChecks }")
    public List<Boolean> hasRights(String memberCrn, List<RightCheck> rightChecks,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return hasRightsNoCache(memberCrn, rightChecks, regionAwareInternalCrnGeneratorFactory);
    }

    /**
     * Retrieves whether the member has the specified rights. This method specifically does not cache
     * results for cases where caching affects application correctness.
     *
     * @param memberCrn   the CRN of the member
     * @param rightChecks the rights to check
     * @return a list of booleans indicating whether the member has the specified rights
     */
    public List<Boolean> hasRightsNoCache(String memberCrn, List<RightCheck> rightChecks,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        LOGGER.info("Checking whether member [{}] has rights [{}]", memberCrn,
                rightChecks.stream().map(this::rightCheckToString).collect(Collectors.toList()));
        checkArgument(rightChecks.stream().map(RightCheck::getResource).allMatch(VALID_AUTHZ_RESOURCE),
                String.format("Following resources are not provided in CRN format: %s.", Joiner.on(",").join(
                        rightChecks.stream().map(RightCheck::getResource).filter(Predicate.not(VALID_AUTHZ_RESOURCE)).collect(Collectors.toList()))));
        if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(memberCrn)) {
            LOGGER.info("InternalCrn has all rights");
            return rightChecks.stream().map(rightCheck -> Boolean.TRUE).collect(Collectors.toList());
        }
        if (!rightChecks.isEmpty()) {
            List<Boolean> retVal;
            if (rightChecks.size() < CHECK_RIGHT_HAS_RIGHTS_TRESHOLD) {
                retVal = rightChecks.stream()
                        .map(check -> makeCheckRightCall(memberCrn, check.getRight(), check.getResource(),
                                regionAwareInternalCrnGeneratorFactory))
                        .collect(Collectors.toList());
            } else {
                AuthorizationClient client = makeAuthorizationClient(regionAwareInternalCrnGeneratorFactory);
                retVal = client.hasRights(memberCrn, rightChecks);
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
    public Map<String, Boolean> hasRights(String memberCrn, List<String> resources, String right,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        List<RightCheck> rightChecks = resources.stream()
                .map(resource -> RightCheck.newBuilder()
                        .setResource(resource)
                        .setRight(right)
                        .build())
                .collect(Collectors.toList());
        LOGGER.debug("Check if {} has rights to resources {}", memberCrn, rightChecks);
        List<Boolean> result = hasRights(memberCrn, rightChecks, regionAwareInternalCrnGeneratorFactory);
        return resources.stream().collect(
                Collectors.toMap(resource -> resource, resource -> result.get(resources.indexOf(resource))));
    }

    public List<Boolean> hasRightsOnResources(String memberCrn, List<String> resourceCrns, String right) {
        checkArgument(resourceCrns.stream().allMatch(VALID_AUTHZ_RESOURCE),
                String.format("Following resources are not provided in CRN format: %s.", Joiner.on(",").join(
                        resourceCrns.stream().filter(Predicate.not(VALID_AUTHZ_RESOURCE)).collect(Collectors.toList()))));
        if (CollectionUtils.isEmpty(resourceCrns)) {
            return List.of();
        }
        if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(memberCrn)) {
            return resourceCrns.stream().map(r -> true).collect(Collectors.toList());
        }
        LOGGER.debug("Check if {} has rights on resources {}", memberCrn, resourceCrns);
        PersonalResourceViewClient client = new PersonalResourceViewClient(channelWrapper.getChannel(), memberCrn, umsClientConfig);
        List<Boolean> retVal = client.hasRightOnResources(memberCrn, right, resourceCrns);
        LOGGER.info("member {} has rights {}", memberCrn, retVal);
        return retVal;
    }

    /**
     * Add role to machine user
     *
     * @param userCrn        actor that will assign the role
     * @param machineUserCrn machine user
     * @param roleCrn        role that will be assigned
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public void assignMachineUserRole(String userCrn, String accountId, String machineUserCrn, String roleCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        try {
            UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
            client.assignMachineUserRole(userCrn, accountId, machineUserCrn, roleCrn);
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

    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public void assignMachineUserResourceRole(String accountId, String machineUserCrn, String resourceRoleCrn, String resourceCrn,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        try {
            UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
            client.assignMachineUserResourceRole(accountId, machineUserCrn, resourceRoleCrn, resourceCrn);
        } catch (StatusRuntimeException ex) {
            if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot assign resource role '%s' to machine user '%s' and resource '%s' as " +
                        "UMS API is UNAVAILABLE at the moment", machineUserCrn, resourceRoleCrn, resourceCrn);
                LOGGER.debug(errMessage, ex);
                throw new UmsOperationException(errMessage, ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Generate access / private keypair
     *
     * @param actorCrn       actor that executes the key generation
     * @param machineUserCrn machine user (owner of the access key)
     * @param accessKeyType  algorithm type used for the access key
     * @return access / private key holder object
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public AltusCredential generateAccessSecretKeyPair(String actorCrn, String accountId, String machineUserCrn,
            AccessKeyType.Value accessKeyType, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        try {
            UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
            LOGGER.info("Generating new access / secret key pair for {}", machineUserCrn);
            CreateAccessKeyResponse accessKeyResponse = client.createAccessPrivateKeyPair(
                    actorCrn, accountId, machineUserCrn, accessKeyType);
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
    public AltusCredential createMachineUserAndGenerateKeys(String machineUserName, String userCrn, String accountId, String roleCrn,
        Map<String, String> resourceRoles, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return createMachineUserAndGenerateKeys(machineUserName, userCrn, accountId, roleCrn, resourceRoles, AccessKeyType.Value.UNSET,
                regionAwareInternalCrnGeneratorFactory);
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
            String roleCrn, Map<String, String> resourceRoles, AccessKeyType.Value accessKeyType,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        Optional<String> machineUserCrn = createMachineUser(machineUserName, userCrn, accountId,
                regionAwareInternalCrnGeneratorFactory);
        if (StringUtils.isNotEmpty(roleCrn)) {
            assignMachineUserRole(userCrn, accountId, machineUserCrn.orElse(null), roleCrn,
                    regionAwareInternalCrnGeneratorFactory);
        }
        if (MapUtils.isNotEmpty(resourceRoles) && machineUserCrn.isPresent()) {
            resourceRoles.forEach((resourceCrn, resourceRoleCrn) -> {
                assignMachineUserResourceRole(accountId, machineUserCrn.get(), resourceRoleCrn, resourceCrn,
                        regionAwareInternalCrnGeneratorFactory);
            });
        }
        return generateAccessSecretKeyPair(userCrn, accountId, machineUserCrn.orElse(null),
                accessKeyType, regionAwareInternalCrnGeneratorFactory);
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
            String machineUserCrn, String accessKeyId, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        List<String> accessKeys = client.listMachineUserAccessKeys(actorCrn, accountId, machineUserCrn, true);
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
    UmsClient makeClient(ManagedChannel channel, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new UmsClient(channel, umsClientConfig, regionAwareInternalCrnGeneratorFactory);
    }

    @VisibleForTesting
    AuthorizationClient makeAuthorizationClient(RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new AuthorizationClient(channelWrapper.getChannel(), umsClientConfig, regionAwareInternalCrnGeneratorFactory);
    }

    /**
     * Queries the metadata file used to configure SSO authentication on clusters.
     *
     * @param accountId the account ID
     * @return metadata as string
     */
    public String getIdentityProviderMetadataXml(String accountId, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Getting IdP metadata through account ID: {}", accountId);
        return client.getIdentityProviderMetadataXml(accountId);
    }

    public Multimap<String, String> listAssignedResourceRoles(String assigneeCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.info("List the assigned resource roles for {} assignee", assigneeCrn);
        UserManagementProto.ListAssignedResourceRolesResponse response =
                client.listAssignedResourceRoles(assigneeCrn);
        Multimap<String, String> resourceRoleAssignments = response.getResourceAssignmentList().stream()
                .collect(Multimaps
                        .toMultimap(
                                UserManagementProto.ResourceAssignment::getResourceCrn,
                                UserManagementProto.ResourceAssignment::getResourceRoleCrn,
                                ArrayListMultimap::create));
        LOGGER.info("Assigned resource roles [{}] for assignee {}", resourceRoleAssignments, assigneeCrn);
        return resourceRoleAssignments;
    }

    public void assignResourceRole(String assigneeCrn, String resourceCrn, String resourceRoleCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.info("Assigning '{}' role at resource '{}' to assignee '{}'...", resourceRoleCrn, resourceCrn, assigneeCrn);
        client.assignResourceRole(assigneeCrn, resourceCrn, resourceRoleCrn);
        LOGGER.info("Resource role assignment has been successfully done ::: Role: {} Resource: {} Assignee: {}",
                resourceRoleCrn, resourceCrn, assigneeCrn);
    }

    public void unassignResourceRole(String assigneeCrn, String resourceCrn, String resourceRoleCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.info("Unassigning '{}' role at resource '{}' from assignee '{}'...", resourceRoleCrn, resourceCrn, assigneeCrn);
        client.unassignResourceRole(assigneeCrn, resourceCrn, resourceRoleCrn);
        LOGGER.info("Resource role unassignment has been successfully done ::: Role: {} Resource: {} Assignee: {}",
                resourceRoleCrn, resourceCrn, assigneeCrn);
    }

    public void notifyResourceDeleted(String resourceCrn,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        try {
            LOGGER.debug("Notify UMS about resource ('{}') was deleted", resourceCrn);
            UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
            client.notifyResourceDeleted(resourceCrn);
            LOGGER.info("Notified UMS about deletion of resource {}", resourceCrn);
        } catch (Exception e) {
            LOGGER.error(String.format("Notifying UMS about deletion of resource %s has failed: ", resourceCrn), e);
        }
    }

    public String setWorkloadAdministrationGroupName(String accountId, UmsVirtualGroupRight right, String resource,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        return client.setWorkloadAdministrationGroupName(
                accountId, right.getRight(), resource).getWorkloadAdministrationGroupName();
    }

    public String getWorkloadAdministrationGroupName(String accountId, UmsVirtualGroupRight right, String resource,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        return client.getWorkloadAdministrationGroupName(
                accountId, right.getRight(), resource).getWorkloadAdministrationGroupName();
    }

    public void deleteWorkloadAdministrationGroupName(String accountId, UmsVirtualGroupRight right, String resource,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        client.deleteWorkloadAdministrationGroupName(accountId, right.getRight(), resource);
    }

    public List<WorkloadAdministrationGroup> listWorkloadAdministrationGroups(String accountId,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        return client.listWorkloadAdministrationGroups(accountId);
    }

    /**
     * Retrieves event generation ids for an account from UMS.
     *
     * @param accountId the account id.
     * @return the user associated with this user CRN
     */
    public GetEventGenerationIdsResponse getEventGenerationIds(String accountId,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Getting event generation ids for account {}.", accountId);
        return client.getEventGenerationIds(accountId);
    }

    /**
     * Retrieves user sync state model from UMS.
     *
     * @param accountId         the account Id
     * @param rightsChecks      list of rights checks for resources. a List is used to preserve order.
     * @param skipCredentials   whether to skip including credentials in the response
     * @return the user sync state for this account and rights checks
     */
    public GetUserSyncStateModelResponse getUserSyncStateModel(String accountId, List<RightsCheck> rightsChecks,
        boolean skipCredentials,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        LOGGER.debug("Retrieving user sync state model for account {}.", accountId);
        return client.getUserSyncStateModel(accountId, rightsChecks, skipCredentials);
    }

    @Cacheable(cacheNames = "umsResourceRolesCache", key = "{ #accountId }")
    public Set<String> getResourceRoles(String accountId) {
        return getResourceRoles(accountId, regionAwareInternalCrnGeneratorFactory);
    }

    @Cacheable(cacheNames = "umsRolesCache", key = "{ #accountId }")
    public Set<String> getRoles(String accountId) {
        return getRoles(accountId, regionAwareInternalCrnGeneratorFactory);
    }

    @Cacheable(cacheNames = "umsResourceRolesCache", key = "{ #accountId }")
    public Set<String> getResourceRoles(String accountId, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        return client.listResourceRoles(accountId);
    }

    @Cacheable(cacheNames = "umsRolesCache", key = "{ #accountId }")
    public Set<String> getRoles(String accountId, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        UmsClient client = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        return client.listRoles(accountId);
    }

}

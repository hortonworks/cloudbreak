package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkArgument;
import static com.sequenceiq.cloudbreak.common.metrics.type.MetricTag.ERROR_CODE;
import static com.sequenceiq.cloudbreak.common.metrics.type.MetricTag.TARGET_API;
import static com.sequenceiq.cloudbreak.common.metrics.type.MetricType.UMS_CALL_FAILED;
import static com.sequenceiq.cloudbreak.common.metrics.type.MetricType.UMS_CALL_SUCCESS;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsErrorException;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.model.UserWithResourceRole;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.Metric;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Component
public class GrpcUmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    private static final Predicate<String> VALID_AUTHZ_RESOURCE = resource -> Crn.isCrn(resource) ||
            StringUtils.isEmpty(resource) || Strings.CS.equals(resource, "*");

    private static final int CHECK_RIGHT_HAS_RIGHTS_TRESHOLD = 3;

    private static final String CHECK_RIGHT = "checkRight";

    private static final String HAS_RIGHTS = "hasRights";

    private static final String HAS_RIGHTS_ON_RESOURCES = "hasRightsOnResources";

    @Qualifier("umsManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private UmsClientConfig umsClientConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    public static GrpcUmsClient createClient(ManagedChannelWrapper channelWrapper, UmsClientConfig clientConfig,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        GrpcUmsClient client = new GrpcUmsClient();
        client.channelWrapper = Preconditions.checkNotNull(channelWrapper, "channelWrapper should not be null.");
        client.umsClientConfig = Preconditions.checkNotNull(clientConfig, "clientConfig should not be null.");
        client.regionAwareInternalCrnGeneratorFactory = Preconditions.checkNotNull(regionAwareInternalCrnGeneratorFactory,
                "regionAwareInternalCrnGeneratorFactory should not be null.");
        return client;
    }

    /**
     * Create new user group if it does not exist.
     *
     * @param accountId          the account ID
     * @param groupName          the newly created group name
     * @return                   the new or existing user group.
     */
    public Group createGroup(String accountId, String groupName) {
        LOGGER.trace("Creating new user group '{}', for account '{}'.", groupName, accountId);
        Group newGroup = makeClient().createGroup(accountId, groupName);
        LOGGER.trace("New user group '{}' has been created for account '{}'.", groupName, accountId);
        return newGroup;
    }

    /**
     * Delete user group if it exist.
     *
     * @param accountId          the account ID
     * @param groupName          the newly created group name
     */
    public void deleteGroup(String accountId, String groupName) {
        LOGGER.trace("Deleting user group '{}', from account '{}'.", groupName, accountId);
        makeClient().deleteGroup(accountId, groupName);
        LOGGER.trace("User group '{}' has been deleted from account '{}'.", groupName, accountId);
    }

    /**
     * Add member to the selected user group if it exist.
     *
     * @param accountId          the account ID
     * @param groupName          the group where user is going to be assigned
     * @param memberCrn          member (e.g., user) CRN
     */
    public void addMemberToGroup(String accountId, String groupName, String memberCrn) {
        LOGGER.trace("Assigning user '{}' to '{}' group, at account '{}'.", memberCrn, groupName, accountId);
        makeClient().addMemberToGroup(accountId, groupName, memberCrn);
        LOGGER.trace("User '{}' has been added to '{}' group successfully.", groupName, accountId);
    }

    /**
     * Remove member from the selected user group if it is exist.
     *
     * @param accountId          the account ID
     * @param groupName          the group where user is going to be assigned
     * @param memberCrn          member (e.g., user) CRN
     */
    public void removeMemberFromGroup(String accountId, String groupName, String memberCrn) {
        LOGGER.trace("Removing user '{}' from '{}' group, at account '{}'.", memberCrn, groupName, accountId);
        makeClient().removeMemberFromGroup(accountId, groupName, memberCrn);
        LOGGER.trace("User '{}' has been removed from '{}' group successfully.", groupName, accountId);
    }

    /**
     * List members from the selected user group if it is exist.
     *
     * @param accountId          the account ID
     * @param groupName          the group where user is going to be assigned
     * @return                   list of user group member CRNs or NULL if the user group does not exist.
     */
    public List<String> listMembersFromGroup(String accountId, String groupName) {
        LOGGER.trace("Listing members from '{}' group, at account '{}'.", groupName, accountId);
        List<String> members = makeClient().listMembersFromGroup(accountId, groupName);
        LOGGER.trace("User group '{}' contains [{}] members at account '{}'.", groupName, members, accountId);
        return members;
    }

    /**
     * Retrieves list of all groups from UMS.
     *
     * @param accountId the account Id
     * @return the list of groups associated with this account
     */
    public List<Group> listAllGroups(String accountId) {
        return listGroups(accountId, null);
    }

    /**
     * Retrieves group list from UMS.
     *
     * @param accountId the account Id
     * @param groupNameOrCrnList the groups to list. if null or empty then all groups will be listed
     * @return the list of groups associated with this account
     */
    public List<Group> listGroups(String accountId, List<String> groupNameOrCrnList) {
        LOGGER.trace("Listing group information for account {}.", accountId);
        List<Group> groups = makeClient().listGroups(accountId, groupNameOrCrnList);
        LOGGER.trace("{} Groups found for account {}", groups.size(), accountId);
        return groups;
    }

    /**
     * Retrieves group list for a specified member from UMS.
     *
     * @param accountId the account Id
     * @param memberCrn the member to list
     * @return the list of group crns associated with this member
     */
    public List<String> listGroupsForMember(String accountId, String memberCrn) {
        LOGGER.trace("Listing group information for member {} in account {}.", memberCrn, accountId);
        List<String> groups = makeClient().listGroupsForMembers(accountId, memberCrn);
        LOGGER.trace("{} Groups found for member {} in account {}", groups.size(), memberCrn, accountId);
        return groups;
    }

    /**
     * Retrieves user details from UMS.
     *
     * @param userCrn   the CRN of the user.
     * @return the user associated with this user CRN
     */
    @Cacheable(cacheNames = "umsUserCache", key = "{ #userCrn }")
    public User getUserDetails(String userCrn) {
        LOGGER.trace("Getting user information for {}.", userCrn);
        User user = makeClient().getUser(userCrn);
        LOGGER.trace("User information retrieved for userCrn: {}", user.getCrn());
        return user;
    }

    /**
     * Set user Workload password.
     *
     * @param userCrn   the CRN of the user.
     * @return the workload credentials associated with this user CRN
     */
    public UserManagementProto.SetActorWorkloadCredentialsResponse setActorWorkloadPassword(String userCrn, String password) {
        String workloadUserName = getUserDetails(userCrn).getWorkloadUsername();
        LOGGER.trace("Setting workload password for user {} with workload name {}", userCrn, workloadUserName);
        UserManagementProto.SetActorWorkloadCredentialsResponse response = makeClient().setActorWorkloadPassword(userCrn, password);
        LOGGER.trace("Workload password has been set for user {} with workload name {}", userCrn, workloadUserName);
        return response;
    }

    /**
     * Retrieves user Workload Credentials.
     *
     * @param userCrn   the CRN of the user.
     * @return the workload credentials associated with this user CRN
     */
    public UserManagementProto.GetActorWorkloadCredentialsResponse getActorWorkloadCredentials(String userCrn) {
        LOGGER.trace("Getting workload credentials for user {}", userCrn);
        UserManagementProto.GetActorWorkloadCredentialsResponse response = makeClient().getActorWorkloadCredentials(userCrn);
        LOGGER.trace("Got workload credentials for user {}", userCrn);
        return response;
    }

    /**
     * Retrieves list of all users from UMS.
     *
     * @param accountId the account Id.
     * @return the list of users associated with this account
     */
    public List<User> listAllUsers(String accountId) {
        return listUsers(accountId, null);
    }

    /**
     * Retrieves user list from UMS.
     *
     * @param accountId the account Id.
     * @param userCrns  the users to list. if null or empty then all users will be listed
     * @return the list of users associated with this account
     */
    public List<User> listUsers(String accountId, List<String> userCrns) {
        LOGGER.trace("Listing user information for account {}.", accountId);
        List<User> users = makeClient().listUsers(accountId, userCrns);
        LOGGER.trace("{} Users found for account {}", users.size(), accountId);
        return users;
    }

    /**
     * Retrieves machine user details from UMS.
     *
     * @param userCrn   the CRN of the user.
     * @return the user associated with this user CRN
     */
    @Cacheable(cacheNames = "umsMachineUserCache", key = "{ #userCrn, #accountId }")
    public MachineUser getMachineUserDetails(String userCrn, String accountId) {
        LOGGER.trace("Getting machine user information for {}.", userCrn);
        MachineUser machineUser = makeClient().getMachineUser(userCrn, accountId);
        LOGGER.trace("MachineUser information retrieved for userCrn: {}", machineUser.getCrn());
        return machineUser;
    }

    /**
     * Set machine user Workload password.
     *
     * @param userCrn   the CRN of the machine user.
     * @return the workload credentials associated with this machine user CRN
     */
    public UserManagementProto.SetActorWorkloadCredentialsResponse setMachineUserWorkloadPassword(String userCrn, String accountId, String password) {
        String workloadUserName = getMachineUserDetails(userCrn, accountId).getWorkloadUsername();
        LOGGER.trace("Setting workload password for machine user {} with workload name {}", userCrn, workloadUserName);
        UserManagementProto.SetActorWorkloadCredentialsResponse response = makeClient().setActorWorkloadPassword(userCrn, password);
        LOGGER.trace("Workload password has been set for machine user {} with workload name {}", userCrn, workloadUserName);
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
    public List<MachineUser> listAllMachineUsers(String accountId, boolean includeInternal, boolean includeWorkloadMachineUsers) {
        return listMachineUsers(accountId, null, includeInternal, includeWorkloadMachineUsers);
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
    public List<MachineUser> listMachineUsers(String accountId, List<String> machineUserCrns, boolean includeInternal, boolean includeWorkloadMachineUsers) {
        LOGGER.trace("Listing machine user information for account {}.", accountId);
        List<MachineUser> machineUsers = makeClient().listMachineUsers(accountId, machineUserCrns, includeInternal, includeWorkloadMachineUsers);
        LOGGER.trace("{} Machine users found for account {}", machineUsers.size(), accountId);
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
    public Optional<String> createMachineUser(String machineUserName, String userCrn, String accountId) {
        try {
            UmsClient client = makeClient();
            LOGGER.trace("Creating machine user {} for {}.", machineUserName, userCrn);
            Optional<String> machineUserCrn = client.createMachineUser(accountId, machineUserName);
            if (machineUserCrn.isEmpty()) {
                MachineUser machineUser = client.getMachineUserForUser(accountId, machineUserName, true, true);
                machineUserCrn = Optional.of(machineUser.getCrn());
            }
            LOGGER.trace("Machine User information retrieved for userCrn: {}", machineUserCrn.orElse(null));
            return machineUserCrn;
        } catch (StatusRuntimeException ex) {
            if (Status.NOT_FOUND.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Machine user with name %s is not found yet", machineUserName);
                LOGGER.trace(errMessage, ex);
                throw new UmsOperationException(errMessage, ex);
            } else if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot create machinue user '%s' for '%s' as " +
                        "UMS API is UNAVAILABLE at the moment", machineUserName, userCrn);
                LOGGER.trace(errMessage, ex);
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
            LOGGER.trace("Creating machine user {} for accountId {}.", machineUserName, accountId);
            MachineUser machineUser = makeClient().getOrCreateMachineUserWithoutAccessKey(accountId, machineUserName);
            LOGGER.trace("Machine User retrieved for machineUserName: {}, machineUser: {}", machineUserName, machineUser);
            return machineUser;
        } catch (StatusRuntimeException ex) {
            if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot create machine user '%s' for '%s' as " +
                        "UMS API is UNAVAILABLE at the moment", machineUserName, accountId);
                LOGGER.trace(errMessage, ex);
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
     * @param accountId      the account Id
     */
    public void deleteMachineUser(String machineUserName, String accountId) {
        LOGGER.trace("Deleting machine user {} for accountId: {}", machineUserName, accountId);
        makeClient().deleteMachineUser(accountId, machineUserName);
    }

    public void deleteAccessKey(String accessKeyId, String accountId) {
        makeClient().deleteAccessKeys(List.of(accessKeyId), accountId);
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
        LOGGER.trace("Listing service principal cloud identities for account {}.", accountId);
        ListServicePrincipalCloudIdentitiesResponse response;
        Optional<PageToken> pageToken = Optional.empty();
        do {
            response = makeClient().listServicePrincipalCloudIdentities(accountId, environmentCrn, pageToken);
            spCloudIds.addAll(response.getServicePrincipalCloudIdentitiesList());
            pageToken = Optional.ofNullable(response.getNextPageToken());
        } while (response.hasNextPageToken());
        LOGGER.trace("{} ServicePrincipalCloudIdentities found for account {}", spCloudIds.size(), accountId);
        return spCloudIds;
    }

    /**
     * Lists the workload administration groups a member belongs to.
     *
     * @param memberCrn the CRN of the user or machine user
     * @return the workload administration groups associated with this user or machine user
     */
    public List<String> listWorkloadAdministrationGroupsForMember(String memberCrn) {
        requireNonNull(memberCrn);
        List<String> wags = new ArrayList<>();
        LOGGER.trace("Getting workload administration groups for member {}", memberCrn);
        ListWorkloadAdministrationGroupsForMemberResponse response;
        Optional<PageToken> pageToken = Optional.empty();
        do {
            response = makeClient().listWorkloadAdministrationGroupsForMember(memberCrn, pageToken);
            wags.addAll(response.getWorkloadAdministrationGroupNameList());
            pageToken = Optional.ofNullable(response.getNextPageToken());
        } while (response.hasNextPageToken());
        LOGGER.trace("{} workload administration groups found for member {}", wags.size(), memberCrn);
        return wags;
    }

    /**
     * Retrieves account details from UMS, which includes the CM license.
     *
     * @param accountId the account ID.
     * @return the account associated with this user CRN
     */
    @Cacheable(cacheNames = "umsAccountCache", key = "{ #accountId }")
    public Account getAccountDetails(String accountId) {
        LOGGER.trace("Getting information for account ID {}.", accountId);
        return makeClient().getAccount(accountId);
    }

    @Cacheable(cacheNames = "umsUserHasRightsForResourceCache", key = "{ #userCrn, #right, #resource }")
    public boolean checkResourceRight(String userCrn, String right, String resource) {
        if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrn)) {
            LOGGER.info("InternalCrn, allow right {} for user {}!", right, userCrn);
            return true;
        }
        return makeCheckRightCallAndHandleExceptions(userCrn, right, resource);
    }

    @Cacheable(cacheNames = "umsUserRightsCache", key = "{ #userCrn, #right }")
    public boolean checkAccountRight(String userCrn, String right) {
        if (RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrn)) {
            LOGGER.info("InternalCrn, allow account right {} for user {}!", right, userCrn);
            return true;
        }
        return makeCheckRightCallAndHandleExceptions(userCrn, right, null);
    }

    private boolean makeCheckRightCallAndHandleExceptions(String userCrn, String right, String resource) {
        checkArgument(VALID_AUTHZ_RESOURCE.test(resource), String.format("Provided resource [%s] is not in CRN format", resource));
        long start = System.currentTimeMillis();
        String resourceOrAccount = resource != null ? resource : "account";
        try {
            LOGGER.info("Checking right {} for user {} on resource {}!", right, userCrn, resourceOrAccount);
            makeAuthorizationClient().checkRight(userCrn, right, resource);
            LOGGER.info("User {} has right {} on resource {}!", userCrn, right, resourceOrAccount);
            recordTimerMetric(UMS_CALL_SUCCESS, duration(start), TARGET_API.name(), CHECK_RIGHT);
            return true;
        } catch (StatusRuntimeException statusRuntimeException) {
            Status.Code statusCode = statusRuntimeException.getStatus().getCode();
            recordTimerMetric(UMS_CALL_FAILED, duration(start), TARGET_API.name(), CHECK_RIGHT, ERROR_CODE.name(), statusCode.name());
            if (Status.Code.PERMISSION_DENIED.equals(statusCode)) {
                LOGGER.error("Checking right {} failed for user {}, thus access is denied! Cause: {}", right, userCrn, statusRuntimeException.getMessage());
                return false;
            } else if (Status.Code.DEADLINE_EXCEEDED.equals(statusCode)) {
                LOGGER.error("Deadline exceeded for check right {} for user {} on resource {}", right, userCrn, resourceOrAccount,
                        statusRuntimeException);
                throw new UmsErrorException("Authorization failed due to user management service call timed out.");
            } else if (Status.Code.NOT_FOUND.equals(statusCode)) {
                LOGGER.error("NOT_FOUND for check right {} for user {} on resource {}, thus access is denied! Cause: {}",
                        right, userCrn, resource, statusRuntimeException.getMessage());
                throw new UnauthorizedException("Authorization failed for user: " + userCrn);
            } else {
                LOGGER.error("Status runtime exception while checking right {} for user {} on resource {}", right, userCrn, resourceOrAccount,
                        statusRuntimeException);
                throw new UmsErrorException("Authorization failed due to user management service call failed.");
            }
        } catch (Exception e) {
            LOGGER.error("Unknown error while checking right {} for user {} on resource {}", right, userCrn, resourceOrAccount, e);
            recordTimerMetric(UMS_CALL_FAILED, duration(start), TARGET_API.name(), CHECK_RIGHT, ERROR_CODE.name(), "OTHER");
            throw new CloudbreakServiceException("Authorization failed due to user management service call failed with error.");
        }
    }

    private Duration duration(long start) {
        return Duration.ofMillis(System.currentTimeMillis() - start);
    }

    /**
     * Retrieves whether the member has the specified rights.
     *
     * @param memberCrn   the CRN of the member
     * @param rightChecks the rights to check
     * @return a list of booleans indicating whether the member has the specified rights
     */
    @Cacheable(cacheNames = "umsUserHasRightsCache", key = "{ #memberCrn, #rightChecks }")
    public List<Boolean> hasRights(String memberCrn, List<RightCheck> rightChecks) {
        return hasRightsNoCache(memberCrn, rightChecks);
    }

    /**
     * Retrieves whether the member has the specified rights. This method specifically does not cache
     * results for cases where caching affects application correctness.
     *
     * @param memberCrn   the CRN of the member
     * @param rightChecks the rights to check
     * @return a list of booleans indicating whether the member has the specified rights
     */
    public List<Boolean> hasRightsNoCache(String memberCrn, List<RightCheck> rightChecks) {
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
                        .map(check -> makeCheckRightCallAndHandleExceptions(memberCrn, check.getRight(), check.getResource()))
                        .collect(Collectors.toList());
            } else {
                retVal = makeHasRightsCallAndHandleExceptions(memberCrn, rightChecks);
            }
            LOGGER.info("member {} has rights {}", memberCrn, retVal);
            return retVal;
        }
        return List.of();
    }

    private List<Boolean> makeHasRightsCallAndHandleExceptions(String memberCrn, List<RightCheck> rightChecks) {
        long start = System.currentTimeMillis();
        try {
            List<Boolean> result = makeAuthorizationClient().hasRights(memberCrn, rightChecks);
            recordTimerMetric(UMS_CALL_SUCCESS, duration(start), TARGET_API.name(), HAS_RIGHTS);
            return result;
        } catch (StatusRuntimeException statusRuntimeException) {
            Status.Code statusCode = statusRuntimeException.getStatus().getCode();
            recordTimerMetric(UMS_CALL_FAILED, duration(start), TARGET_API.name(), HAS_RIGHTS, ERROR_CODE.name(), statusCode.name());
            if (Status.Code.DEADLINE_EXCEEDED.equals(statusCode)) {
                LOGGER.error("Deadline exceeded for hasRights for actor {} and rights {}", memberCrn, rightChecks, statusRuntimeException);
                throw new UmsErrorException("Authorization failed due to user management service call timed out.");
            } else if (Status.Code.NOT_FOUND.equals(statusCode)) {
                LOGGER.error("NOT_FOUND for hasRights for actor {} and rights {}", memberCrn, rightChecks, statusRuntimeException);
                throw new UnauthorizedException("Authorization failed for user: " + memberCrn);
            } else {
                LOGGER.error("Status runtime exception while checking hasRights for actor {} and rights {}", memberCrn, rightChecks, statusRuntimeException);
                throw new UmsErrorException("Authorization failed due to user management service call failed.");
            }
        } catch (Exception e) {
            LOGGER.error("Unknown error while checking hasRights for actor {} and rights {}", memberCrn, rightChecks, e);
            recordTimerMetric(UMS_CALL_FAILED, duration(start), TARGET_API.name(), HAS_RIGHTS, ERROR_CODE.name(), "OTHER");
            throw new CloudbreakServiceException("Authorization failed due to user management service call failed with error.");
        }
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
    public Map<String, Boolean> hasRights(String memberCrn, List<String> resources, String right) {
        List<RightCheck> rightChecks = resources.stream()
                .map(resource -> RightCheck.newBuilder()
                        .setResource(resource)
                        .setRight(right)
                        .build())
                .collect(Collectors.toList());
        LOGGER.trace("Check if {} has rights to resources {}", memberCrn, rightChecks);
        List<Boolean> result = hasRights(memberCrn, rightChecks);
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
        long start = System.currentTimeMillis();
        try {
            List<Boolean> retVal = makePersonalResourceViewClient().hasResourcesByRight(memberCrn, right, resourceCrns);
            LOGGER.info("Member {} has rights {}", memberCrn, retVal);
            recordTimerMetric(UMS_CALL_SUCCESS, duration(start), TARGET_API.name(), HAS_RIGHTS_ON_RESOURCES);
            return retVal;
        } catch (StatusRuntimeException statusRuntimeException) {
            Status.Code statusCode = statusRuntimeException.getStatus().getCode();
            recordTimerMetric(UMS_CALL_FAILED, duration(start), TARGET_API.name(), HAS_RIGHTS_ON_RESOURCES, ERROR_CODE.name(), statusCode.name());
            if (Status.Code.DEADLINE_EXCEEDED.equals(statusCode)) {
                LOGGER.error("Deadline exceeded for hasRightOnResources for actor {} and right {} and resources {}", memberCrn, right, resourceCrns,
                        statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call timed out.");
            } else if (Status.Code.NOT_FOUND.equals(statusCode)) {
                LOGGER.error("NOT_FOUND error happened for hasRightOnResources for actor {} and right {} and resources {}! Cause: {}",
                        memberCrn, right, resourceCrns, statusRuntimeException.getMessage());
                throw new UnauthorizedException("Authorization failed for user: " + memberCrn);
            } else {
                LOGGER.error("Status runtime exception while checking hasRightOnResources for actor {} and right {} and resources {}", memberCrn, right,
                        resourceCrns, statusRuntimeException);
                throw new CloudbreakServiceException("Authorization failed due to user management service call failed.");
            }
        } catch (Exception e) {
            LOGGER.error("Unknown error while checking hasRightOnResources for actor {} and right {} and resources {}", memberCrn, right, resourceCrns, e);
            recordTimerMetric(UMS_CALL_FAILED, duration(start), TARGET_API.name(), HAS_RIGHTS_ON_RESOURCES, ERROR_CODE.name(), "OTHER");
            throw new CloudbreakServiceException("Authorization failed due to user management service call failed with error.");
        }
    }

    /**
     * Add role to machine user
     *
     * @param accountId      the account ID
     * @param machineUserCrn machine user
     * @param roleCrn        role that will be assigned
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public void assignMachineUserRole(String accountId, String machineUserCrn, String roleCrn) {
        try {
            makeClient().assignMachineUserRole(accountId, machineUserCrn, roleCrn);
        } catch (StatusRuntimeException ex) {
            if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot assign role '%s' to machine user '%s' as " +
                        "UMS API is UNAVAILABLE at the moment", machineUserCrn, roleCrn);
                LOGGER.trace(errMessage, ex);
                throw new UmsOperationException(errMessage, ex);
            } else {
                throw ex;
            }
        }
    }

    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public void assignMachineUserResourceRole(String accountId, String machineUserCrn, String resourceRoleCrn, String resourceCrn) {
        try {
            makeClient().assignMachineUserResourceRole(accountId, machineUserCrn, resourceRoleCrn, resourceCrn);
        } catch (StatusRuntimeException ex) {
            if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot assign resource role '%s' to machine user '%s' and resource '%s' as " +
                        "UMS API is UNAVAILABLE at the moment", machineUserCrn, resourceRoleCrn, resourceCrn);
                LOGGER.trace(errMessage, ex);
                throw new UmsOperationException(errMessage, ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Generate access / private keypair
     *
     * @param machineUserCrn machine user (owner of the access key)
     * @param accessKeyType  algorithm type used for the access key
     * @return access / private key holder object
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public AltusCredential generateAccessSecretKeyPair(String accountId, String machineUserCrn, AccessKeyType.Value accessKeyType) {
        try {
            LOGGER.info("Generating new access / secret key pair for {}", machineUserCrn);
            CreateAccessKeyResponse accessKeyResponse = makeClient().createAccessPrivateKeyPair(accountId, machineUserCrn, accessKeyType);
            return new AltusCredential(accessKeyResponse.getAccessKey().getAccessKeyId(), accessKeyResponse.getPrivateKey().toCharArray());
        } catch (StatusRuntimeException ex) {
            if (Status.UNAVAILABLE.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Cannot generate access key pair for machine user '%s' as UMS API is UNAVAILABLE at the moment",
                        machineUserCrn);
                LOGGER.trace(errMessage, ex);
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
            Map<String, String> resourceRoles) {
        return createMachineUserAndGenerateKeys(machineUserName, userCrn, accountId, roleCrn, resourceRoles, AccessKeyType.Value.UNSET);
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
            String roleCrn, Map<String, String> resourceRoles, AccessKeyType.Value accessKeyType) {
        return createMachineUserAndGenerateKeys(machineUserName, userCrn, accountId, Set.of(roleCrn), resourceRoles, accessKeyType);
    }

    /**
     * Generate new machine user (if it is needed) and access api key for this user.
     * Also assign built-in dabaus uploader role for the machine user (if the role is not empty).
     *
     * @param machineUserName machine user name
     * @param userCrn         crn of the actor
     * @param roleCrns        crns of the roles
     * @param accessKeyType   algorithm type used for the access key
     * @return credential (access/secret keypair)
     */
    @Retryable(value = UmsOperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 5000))
    public AltusCredential createMachineUserAndGenerateKeys(String machineUserName, String userCrn, String accountId,
            Set<String> roleCrns, Map<String, String> resourceRoles, AccessKeyType.Value accessKeyType) {
        Optional<String> machineUserCrn = createMachineUser(machineUserName, userCrn, accountId);
        if (CollectionUtils.isNotEmpty(roleCrns)) {
            roleCrns.forEach(roleCrn -> assignMachineUserRole(accountId, machineUserCrn.orElse(null), roleCrn));
        }
        if (MapUtils.isNotEmpty(resourceRoles) && machineUserCrn.isPresent()) {
            resourceRoles
                    .forEach((resourceCrn, resourceRoleCrn) -> assignMachineUserResourceRole(accountId, machineUserCrn.get(), resourceRoleCrn, resourceCrn));
        }
        return generateAccessSecretKeyPair(accountId, machineUserCrn.orElse(null), accessKeyType);
    }

    /**
     * Check that machine user has a specific access key in UMS
     *
     * @param accountId      the account ID
     * @param machineUserCrn machine user crn that own the access key
     * @param accessKeyId    access key id that we need to check
     * @return result that is true if the machine user has the queried access key in UMS
     */
    public boolean doesMachineUserHasAccessKey(String accountId, String machineUserCrn, String accessKeyId) {
        List<String> accessKeys = makeClient().listMachineUserAccessKeys(accountId, machineUserCrn, true);
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
        LOGGER.trace("Try getting anonymization rules for {} by {}", accountId, actorCrn);
        return new ArrayList<>();
    }

    @VisibleForTesting
    UmsClient makeClient() {
        return new UmsClient(channelWrapper.getChannel(), umsClientConfig, regionAwareInternalCrnGeneratorFactory);
    }

    @VisibleForTesting
    AuthorizationClient makeAuthorizationClient() {
        return new AuthorizationClient(channelWrapper.getChannel(), umsClientConfig, regionAwareInternalCrnGeneratorFactory);
    }

    private PersonalResourceViewClient makePersonalResourceViewClient() {
        return new PersonalResourceViewClient(channelWrapper.getChannel(), umsClientConfig, regionAwareInternalCrnGeneratorFactory);
    }

    /**
     * Queries the metadata file used to configure SSO authentication on clusters.
     *
     * @param accountId the account ID
     * @return metadata as string
     */
    public String getIdentityProviderMetadataXml(String accountId) {
        LOGGER.trace("Getting IdP metadata through account ID: {}", accountId);
        return makeClient().getIdentityProviderMetadataXml(accountId);
    }

    public Multimap<String, String> listAssignedResourceRoles(String assigneeCrn) {
        LOGGER.info("List the assigned resource roles for {} assignee", assigneeCrn);
        UserManagementProto.ListAssignedResourceRolesResponse response = makeClient().listAssignedResourceRoles(assigneeCrn);
        Multimap<String, String> resourceRoleAssignments = response.getResourceAssignmentList().stream()
                .collect(Multimaps
                        .toMultimap(
                                UserManagementProto.ResourceAssignment::getResourceCrn,
                                UserManagementProto.ResourceAssignment::getResourceRoleCrn,
                                ArrayListMultimap::create));
        LOGGER.info("Assigned resource roles [{}] for assignee {}", resourceRoleAssignments, assigneeCrn);
        return resourceRoleAssignments;
    }

    public List<UserManagementProto.ResourceAssignee> listResourceAssignees(String resourceCrn) {
        LOGGER.info("Listing all assignees for resource {}", resourceCrn);
        List<UserManagementProto.ResourceAssignee> resourceAssignees = makeClient().listResourceAssignees(resourceCrn);
        LOGGER.info("{} assignees found for resource {}", resourceAssignees.size(), resourceCrn);
        return resourceAssignees;
    }

    public void assignResourceRole(String assigneeCrn, String resourceCrn, String resourceRoleCrn) {
        LOGGER.info("Assigning '{}' role at resource '{}' to assignee '{}'...", resourceRoleCrn, resourceCrn, assigneeCrn);
        makeClient().assignResourceRole(assigneeCrn, resourceCrn, resourceRoleCrn);
        LOGGER.info("Resource role assignment has been successfully done ::: Role: {} Resource: {} Assignee: {}",
                resourceRoleCrn, resourceCrn, assigneeCrn);
    }

    public void unassignResourceRole(String assigneeCrn, String resourceCrn, String resourceRoleCrn) {
        LOGGER.info("Unassigning '{}' role at resource '{}' from assignee '{}'...", resourceRoleCrn, resourceCrn, assigneeCrn);
        makeClient().unassignResourceRole(assigneeCrn, resourceCrn, resourceRoleCrn);
        LOGGER.info("Resource role unassignment has been successfully done ::: Role: {} Resource: {} Assignee: {}", resourceRoleCrn, resourceCrn, assigneeCrn);
    }

    public void notifyResourceDeleted(String resourceCrn) {
        try {
            LOGGER.trace("Notify UMS about resource ('{}') was deleted", resourceCrn);
            makeClient().notifyResourceDeleted(resourceCrn);
            LOGGER.info("Notified UMS about deletion of resource {}", resourceCrn);
        } catch (Exception e) {
            LOGGER.error(String.format("Notifying UMS about deletion of resource %s has failed: ", resourceCrn), e);
        }
    }

    public String setWorkloadAdministrationGroupName(String accountId, UmsVirtualGroupRight right, String resource) {
        return makeClient().setWorkloadAdministrationGroupName(accountId, right.getRight(), resource).getWorkloadAdministrationGroupName();
    }

    public String getWorkloadAdministrationGroupName(String accountId, UmsVirtualGroupRight right, String resource) {
        return makeClient().getWorkloadAdministrationGroupName(accountId, right.getRight(), resource).getWorkloadAdministrationGroupName();
    }

    @Retryable(retryFor = RuntimeException.class, maxAttempts = 5, backoff = @Backoff(delay = 2000))
    public void deleteWorkloadAdministrationGroupName(String accountId, UmsVirtualGroupRight right, String resource) {
        try {
            makeClient().deleteWorkloadAdministrationGroupName(accountId, right.getRight(), resource);
        } catch (StatusRuntimeException e) {
            if (Status.NOT_FOUND.getCode().equals(e.getStatus().getCode())) {
                LOGGER.debug("UMS virtualgroup not found, deletion skipped.");
            } else {
                throw e;
            }
        }
    }

    public List<WorkloadAdministrationGroup> listWorkloadAdministrationGroups(String accountId) {
        return makeClient().listWorkloadAdministrationGroups(accountId);
    }

    /**
     * Retrieves event generation ids for an account from UMS.
     *
     * @param accountId the account id.
     * @return the user associated with this user CRN
     */
    public GetEventGenerationIdsResponse getEventGenerationIds(String accountId) {
        LOGGER.trace("Getting event generation ids for account {}.", accountId);
        return makeClient().getEventGenerationIds(accountId);
    }

    /**
     * Retrieves user sync state model from UMS.
     *
     * @param accountId         the account Id
     * @param rightsChecks      list of rights checks for resources. a List is used to preserve order.
     * @param skipCredentials   whether to skip including credentials in the response
     * @return the user sync state for this account and rights checks
     */
    public GetUserSyncStateModelResponse getUserSyncStateModel(String accountId, List<RightsCheck> rightsChecks, boolean skipCredentials) {
        LOGGER.trace("Retrieving user sync state model for account {}.", accountId);
        return makeClient().getUserSyncStateModel(accountId, rightsChecks, skipCredentials);
    }

    @Cacheable(cacheNames = "umsResourceRolesCache", key = "{ #accountId }")
    public Set<String> getResourceRoles(String accountId) {
        return makeClient().listResourceRoles(accountId);
    }

    @Cacheable(cacheNames = "umsRolesCache", key = "{ #accountId }")
    public Set<String> getRoles(String accountId) {
        return makeClient().listRoles(accountId);
    }

    private void recordTimerMetric(Metric metric, Duration duration, String... tags) {
        if (metricService != null) {
            metricService.recordTimerMetric(metric, duration, tags);
        }
    }

    public void grantEntitlement(String accountId, String entitlementName) {
        makeClient().grantEntitlement(accountId, entitlementName);
    }

    public void revokeEntitlement(String accountId, String entitlementName) {
        makeClient().revokeEntitlement(accountId, entitlementName);
    }

    /**
     * Lists users assigned to a resource with specific resource roles.
     *
     * @param resourceRoleCrns list of resource role CRNs to filter by
     * @param resourceCrn the CRN of the resource to check
     * @return list of users with their assigned resource roles that match the specified resource role CRNs
     */
    public List<UserWithResourceRole> listUsersWithResourceRoles(Set<String> resourceRoleCrns, String resourceCrn) {
        LOGGER.info("Listing users with resource roles {} for resource {}", resourceRoleCrns, resourceCrn);

        List<UserManagementProto.ResourceAssignee> resourceAssignees = listResourceAssignees(resourceCrn);
        List<UserWithResourceRole> usersWithRoles = new ArrayList<>();

        for (UserManagementProto.ResourceAssignee assignee : resourceAssignees) {
            String assigneeCrn = assignee.getAssigneeCrn();
            String resourceRoleCrn = assignee.getResourceRoleCrn();

            // Check if this assignee has one of the target resource roles
            if (resourceRoleCrns.contains(resourceRoleCrn)) {
                try {
                    // Parse the CRN to check if it's a user
                    Crn crn = Crn.safeFromString(assigneeCrn);
                    if (Crn.ResourceType.USER.equals(crn.getResourceType())) {
                        usersWithRoles.add(new UserWithResourceRole(assigneeCrn, resourceRoleCrn));
                        LOGGER.debug("Found user {} with resource role {} for resource {}",
                                assigneeCrn, resourceRoleCrn, resourceCrn);
                    } else {
                        LOGGER.debug("Skipping non-user assignee {} (type: {}) for resource {}",
                                assigneeCrn, crn.getResourceType(), resourceCrn);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to process assignee {} for resource {}: {}",
                            assigneeCrn, resourceCrn, e.getMessage());
                }
            }
        }

        LOGGER.info("Found {} users with matching resource roles for resource {}", usersWithRoles.size(), resourceCrn);
        return usersWithRoles;
    }
}

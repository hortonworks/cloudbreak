package com.sequenceiq.cloudbreak.auth.altus;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.cloudera.thunderhead.service.common.paging.PagingProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ServicePrincipalCloudIdentities;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Component
public class GrpcUmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    private static final String ACCOUNT_IN_IAM_CRNS = "altus";

    @Inject
    private UmsConfig umsConfig;

    @Inject
    private UmsClientConfig umsClientConfig;

    public static GrpcUmsClient createClient(UmsConfig config, UmsClientConfig clientConfig) {
        GrpcUmsClient client = new GrpcUmsClient();
        client.setUmsConfig(config);
        client.setUmsClientConfig(clientConfig);
        return client;
    }

    /**
     * Retrieves list of all groups from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @return the list of groups associated with this account
     */
    public List<Group> listAllGroups(String actorCrn, String accountId, Optional<String> requestId) {
        return listGroups(actorCrn, accountId, null, requestId);
    }

    /**
     * Retrieves group list from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @param groupCrns the groups to list. if null or empty then all groups will be listed
     * @return the list of groups associated with this account
     */
    public List<Group> listGroups(String actorCrn, String accountId, List<String> groupCrns, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Listing group information for account {} using request ID {}", accountId, requestId);
            List<Group> groups = client.listGroups(requestId.orElse(UUID.randomUUID().toString()), accountId, groupCrns);
            LOGGER.debug("{} Groups found for account {}", groups.size(), accountId);
            return groups;
        }
    }

    /**
     * Retrieves group list for a specified member from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @param memberCrn the member to list
     * @return the list of group crns associated with this member
     */
    public List<String> listGroupsForMember(String actorCrn, String accountId, String memberCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Listing group information for member {} in account {} using request ID {}", memberCrn, accountId, requestId);
            List<String> groups = client.listGroupsForMembers(requestId.orElse(UUID.randomUUID().toString()), accountId, memberCrn);
            LOGGER.debug("{} Groups found for member {} in account {}", groups.size(), memberCrn, accountId);
            return groups;
        }
    }

    /**
     * Retrieves user details from UMS.
     *
     * @param actorCrn  the CRN of the actor
     * @param userCrn   the CRN of the user
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    @Cacheable(cacheNames = "umsUserCache", key = "{ #actorCrn, #userCrn }")
    public User getUserDetails(String actorCrn, String userCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting user information for {} using request ID {}", userCrn, requestId);
            User user = client.getUser(requestId.orElse(UUID.randomUUID().toString()), userCrn);
            LOGGER.debug("User information retrieved for userCrn: {}", user.getCrn());
            return user;
        }
    }

    /**
     * Retrieves user Workload Credentials.
     *
     * @param actorCrn  the CRN of the actor
     * @param userCrn   the CRN of the user
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    public GetActorWorkloadCredentialsResponse getActorWorkloadCredentials(String actorCrn, String userCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting workload credentials for user {}", userCrn);
            GetActorWorkloadCredentialsResponse response = client.getActorWorkloadCredentials(requestId.orElse(UUID.randomUUID().toString()), userCrn);
            LOGGER.debug("Got workload credentials for user {}", userCrn);
            return response;
        }
    }

    /**
     * Retrieves list of all users from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @return the list of users associated with this account
     */
    public List<User> listAllUsers(String actorCrn, String accountId, Optional<String> requestId) {
        return listUsers(actorCrn, accountId, null, requestId);
    }

    /**
     * Retrieves user list from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @param userCrns  the users to list. if null or empty then all users will be listed
     * @return the list of users associated with this account
     */
    public List<User> listUsers(String actorCrn, String accountId, List<String> userCrns, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Listing user information for account {} using request ID {}", accountId, requestId);
            List<User> users = client.listUsers(requestId.orElse(UUID.randomUUID().toString()), accountId, userCrns);
            LOGGER.debug("{} Users found for account {}", users.size(), accountId);
            return users;
        }
    }

    /**
     * Retrieves machine user details from UMS.
     *
     * @param actorCrn  the CRN of the actor
     * @param userCrn   the CRN of the user
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    @Cacheable(cacheNames = "umsMachineUserCache", key = "{ #actorCrn, #userCrn }")
    public MachineUser getMachineUserDetails(String actorCrn, String userCrn, String accountId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting machine user information for {} using request ID {}", userCrn, requestId);
            MachineUser machineUser = client.getMachineUser(requestId.orElse(UUID.randomUUID().toString()), userCrn, accountId);
            LOGGER.debug("MachineUser information retrieved for userCrn: {}", machineUser.getCrn());
            return machineUser;
        }
    }

    /**
     * Retrieves list of all machine users from UMS.
     *
     * @param accountId the account Id
     * @param includeInternal whether to include internal machine users
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    public List<MachineUser> listAllMachineUsers(
            String actorCrn, String accountId, boolean includeInternal, Optional<String> requestId) {
        return listMachineUsers(actorCrn, accountId, null, includeInternal, requestId);
    }

    /**
     * Retrieves machine user list from UMS.
     *
     * @param accountId       the account Id
     * @param machineUserCrns machine users to list. if null or empty then all machine users will be listed
     * @param includeInternal whether to include internal machine users
     * @param requestId       an optional request Id
     * @return the user associated with this user CRN
     */
    public List<MachineUser> listMachineUsers(
            String actorCrn, String accountId, List<String> machineUserCrns, boolean includeInternal, Optional<String> requestId) {

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Listing machine user information for account {} using request ID {}", accountId, requestId);
            List<MachineUser> machineUsers = client.listMachineUsers(requestId.orElse(UUID.randomUUID().toString()),
                    accountId, machineUserCrns, includeInternal);
            LOGGER.debug("{} Machine users found for account {}", machineUsers.size(), accountId);
            return machineUsers;
        }
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
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            String generatedRequestId = requestId.orElse(UUID.randomUUID().toString());
            LOGGER.debug("Creating machine user {} for {} using request ID {}", machineUserName, userCrn, generatedRequestId);
            Optional<String> machineUserCrn = client.createMachineUser(requestId.orElse(UUID.randomUUID().toString()), userCrn, accountId, machineUserName);
            if (machineUserCrn.isEmpty()) {
                MachineUser machineUser = client.getMachineUserForUser(requestId.orElse(UUID.randomUUID().toString()), userCrn, accountId, machineUserName);
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
     * Delete machine user
     *
     * @param machineUserName user name that should be deleted
     * @param userCrn         actor
     * @param requestId       request id for deleting machine user
     */
    public void deleteMachineUser(String machineUserName, String userCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            String generatedRequestId = requestId.orElse(UUID.randomUUID().toString());
            LOGGER.debug("Deleting machine user {} by {} using request ID {}", machineUserName, userCrn, generatedRequestId);
            client.deleteMachineUser(generatedRequestId, userCrn, machineUserName);
        }
    }

    /**
     * Retrieves list of service principal cloud identities for an environment from UMS.
     *
     * @param accountId      the account Id
     * @param environmentCrn the environment crn
     * @param requestId      an optional request Id
     * @return list of service principal cloud identities for an environment
     */
    public List<ServicePrincipalCloudIdentities> listServicePrincipalCloudIdentities(
            String actorCrn, String accountId, String environmentCrn, Optional<String> requestId) {

        List<ServicePrincipalCloudIdentities> spCloudIds = new ArrayList<>();

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Listing service principal cloud identities for account {} using request ID {}", accountId, requestId);
            UserManagementProto.ListServicePrincipalCloudIdentitiesResponse response;
            Optional<PagingProto.PageToken> pageToken = Optional.empty();
            do {
                response = client.listServicePrincipalCloudIdentities(
                        requestId.orElse(UUID.randomUUID().toString()), accountId, environmentCrn, pageToken);
                spCloudIds.addAll(response.getServicePrincipalCloudIdentitiesList());
                pageToken = Optional.ofNullable(response.getNextPageToken());
            } while (response.hasNextPageToken());
        }
        LOGGER.debug("{} ServicePrincipalCloudIdentities found for account {}", spCloudIds.size(), accountId);
        return spCloudIds;
    }

    /**
     * Gets rights for user or machine user
     *
     * @param actorCrn       the CRN of the actor
     * @param userCrn        the CRN of the user or machine user
     * @param environmentCrn the CRN of the environment
     * @param requestId      request id for getting rights
     * @return the rights associated with this user or machine user
     */
    public GetRightsResponse getRightsForUser(String actorCrn, String userCrn, String environmentCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting rights for user {} in environment {}", userCrn, environmentCrn);
            return client.getRightsForUser(requestId.orElse(UUID.randomUUID().toString()), userCrn, environmentCrn);
        }
    }

    /**
     * Lists the workload administration groups a member belongs to.
     *
     * @param actorCrn  the CRN of the actor
     * @param memberCrn the CRN of the user or machine user
     * @param requestId request id for getting rights
     * @return the workload administration groups associated with this user or machine user
     */
    public List<String> listWorkloadAdministrationGroupsForMember(
            String actorCrn, String memberCrn, Optional<String> requestId) {
        requireNonNull(memberCrn);

        List<String> wags = new ArrayList<>();

        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting workload administration groups for member {}", memberCrn);
            UserManagementProto.ListWorkloadAdministrationGroupsForMemberResponse response;
            Optional<PagingProto.PageToken> pageToken = Optional.empty();
            do {
                response = client.listWorkloadAdministrationGroupsForMember(
                        requestId.orElse(UUID.randomUUID().toString()), memberCrn, pageToken);
                wags.addAll(response.getWorkloadAdministrationGroupNameList());
                pageToken = Optional.ofNullable(response.getNextPageToken());
            } while (response.hasNextPageToken());
        }
        LOGGER.debug("{} workload administration groups found for member {}", wags.size(), memberCrn);
        return wags;
    }

    /**
     * Retrieves account details from UMS, which includes the CM license.
     *
     * @param actorCrn  the CRN of the actor
     * @param accountId the account ID
     * @param requestId an optional request Id
     * @return the account associated with this user CRN
     */
    @Cacheable(cacheNames = "umsAccountCache", key = "{ #actorCrn, #accountId }")
    public Account getAccountDetails(String actorCrn, String accountId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting information for account ID {} using request ID {}", accountId, requestId);
            return client.getAccount(requestId.orElse(UUID.randomUUID().toString()), accountId);
        }
    }

    @Cacheable(cacheNames = "umsUserRoleAssigmentsCache", key = "{ #actorCrn, #userCrn }")
    public List<UserManagementProto.ResourceAssignment> listResourceRoleAssigments(String actorCrn, String userCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            return client.listAssigmentsOfUser(requestId.orElse(UUID.randomUUID().toString()), userCrn);
        }
    }

    //"#value.concat(#fieldId).concat(#projectId)"
//    @Cacheable(cacheNames = "umsUserRightsCache", key = "{ #actorCrn, #userCrn, #right, #resource }")
    @Cacheable(cacheNames = "umsUserRightsCache", key = "{ #userCrn, #right, #resource }")
    public boolean checkRight(String actorCrn, String userCrn, String right, String resource, Optional<String> requestId) {
        if (InternalCrnBuilder.isInternalCrn(userCrn)) {
            LOGGER.info("InternalCrn, allow right {} for user {}!", right, userCrn);
            return true;
        }
        if (!isAuthorizationEntitlementRegistered(actorCrn, ThreadBasedUserCrnProvider.getAccountId())) {
            if (isReadRight(right)) {
                LOGGER.info("In account {} authorization related entitlement disabled, thus skipping permission check!!",
                        ThreadBasedUserCrnProvider.getAccountId());
                return true;
            } else {
                // if legacy authz then we will check permission on account level
                resource = null;
            }
        }
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            AuthorizationClient client = new AuthorizationClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Checking right {} for user {} on resource {}!", right, userCrn, resource != null ? resource : "account");
            client.checkRight(requestId.orElse(UUID.randomUUID().toString()), userCrn, right, resource);
            LOGGER.info("User {} has right {} on resource {}!", userCrn, right, resource != null ? resource : "account");
            return true;
        } catch (Exception e) {
            LOGGER.error("Checking right {} failed for user {}, thus access is denied! Cause: {}", right, userCrn, e.getMessage());
            return false;
        }
    }

    @Cacheable(cacheNames = "umsUserRightsCache", key = "{ #actorCrn, #userCrn, #right }")
    public boolean checkRight(String actorCrn, String userCrn, String right, Optional<String> requestId) {
        return checkRight(actorCrn, userCrn, right, null, requestId);
    }

    /**
     * Retrieves whether the member has the specified rights.
     *
     * @param actorCrn    the CRN of the actor
     * @param memberCrn   the CRN of the member
     * @param rightChecks the rights to check
     * @param requestId   an optional request id
     * @return a list of booleans indicating whether the member has the specified rights
     */
    public List<Boolean> hasRights(String actorCrn, String memberCrn, List<AuthorizationProto.RightCheck> rightChecks, Optional<String> requestId) {
        LOGGER.info("Checking whether member [{}] has rights [{}]", memberCrn,
                rightChecks.stream().map(this::rightCheckToString).collect(Collectors.toList()));
        if (InternalCrnBuilder.isInternalCrn(memberCrn)) {
            LOGGER.info("InternalCrn has all rights");
            return rightChecks.stream().map(rightCheck -> Boolean.TRUE).collect(Collectors.toList());
        }
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            if (!rightChecks.isEmpty()) {
                AuthorizationClient client = new AuthorizationClient(channelWrapper.getChannel(), actorCrn);
                List<Boolean> retVal = client.hasRights(requestId.orElse(UUID.randomUUID().toString()), memberCrn, rightChecks);
                LOGGER.info("member {} has rights {}", memberCrn, retVal);
                return retVal;
            }
            return Collections.emptyList();
        }
    }

    public String rightCheckToString(AuthorizationProto.RightCheck rightCheck) {
        String right = rightCheck.getRight();
        String resource = "account";
        if (StringUtils.isNotBlank(rightCheck.getResource())) {
            resource = rightCheck.getResource();
        }
        return Joiner.on(" ").join(Lists.newArrayList(right, "for", resource));
    }

    public Map<String, Boolean> hasRights(String actorCrn, String memberCrn, List<String> resources, String right, Optional<String> requestId) {
        List<AuthorizationProto.RightCheck> rightChecks = resources.stream()
                .map(resource -> AuthorizationProto.RightCheck.newBuilder()
                        .setResource(resource)
                        .setRight(right)
                        .build())
                .collect(Collectors.toList());
        LOGGER.debug("Check if {} has rights to resources {}:", actorCrn, rightChecks);
        List<Boolean> result = hasRights(actorCrn, memberCrn, rightChecks, requestId);
        return resources.stream().collect(
                Collectors.toMap(resource -> resource, resource -> result.get(resources.indexOf(resource))));
    }

    @Cacheable(cacheNames = "umsResourceAssigneesCache", key = "{ #actorCrn, #userCrn, #resourceCrn }")
    public List<UserManagementProto.ResourceAssignee> listAssigneesOfResource(String actorCrn, String userCrn,
            String resourceCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            return client.listResourceAssigneesForResource(requestId.orElse(UUID.randomUUID().toString()), resourceCrn);
        }
    }

    @Cacheable(cacheNames = "umsAuthorizationEntitlementRegisteredCache", key = "{ #actorCrn, #accountId }")
    public boolean isAuthorizationEntitlementRegistered(String actorCrn, String accountId) {
        if (StringUtils.equals(accountId, InternalCrnBuilder.INTERNAL_ACCOUNT)) {
            return false;
        }
        return getAccountDetails(actorCrn, accountId, MDCUtils.getRequestId()).getEntitlementsList()
                .stream()
                .map(e -> e.getEntitlementName().toUpperCase())
                .anyMatch(e -> e.equalsIgnoreCase("CB_AUTHZ_POWER_USERS"));
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
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            client.assignMachineUserRole(requestId.orElse(UUID.randomUUID().toString()),
                    userCrn, accountId, machineUserCrn, roleCrn);
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
     * @param userCrn        actor that will unassign the role
     * @param machineUserCrn machine user
     * @param roleCrn        role that will be removed
     * @param requestId      id for the request
     */
    public void unassignMachineUserRole(String userCrn, String machineUserCrn, String roleCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            client.unassignMachineUserRole(requestId.orElse(UUID.randomUUID().toString()),
                    userCrn, machineUserCrn, roleCrn);
        }
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
            Optional<String> requestId, UserManagementProto.AccessKeyType.Value accessKeyType) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Generating new access / secret key pair for {}", machineUserCrn);
            CreateAccessKeyResponse accessKeyResponse = client.createAccessPrivateKeyPair(
                    requestId.orElse(UUID.randomUUID().toString()), actorCrn, accountId, machineUserCrn, accessKeyType);
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
        return createMachineUserAndGenerateKeys(machineUserName, userCrn, accountId, roleCrn, UserManagementProto.AccessKeyType.Value.UNSET);
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
            String roleCrn, UserManagementProto.AccessKeyType.Value accessKeyType) {
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
            unassignMachineUserRole(userCrn, machineUserName, roleCrn, MDCUtils.getRequestId());
        }
        deleteMachineUserAccessKeys(userCrn, accountId, machineUserName, MDCUtils.getRequestId());
        deleteMachineUser(machineUserName, userCrn, MDCUtils.getRequestId());
    }

    /**
     * Delete all access key for machine user
     *
     * @param actorCrn       actor that executes the deletions
     * @param machineUserCrn machine user
     * @param requestId      id for the request
     */
    public void deleteMachineUserAccessKeys(String actorCrn, String accountId, String machineUserCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Getting access keys for {}", machineUserCrn);
            List<String> accessKeys = client.listMachineUserAccessKeys(requestId.orElse(UUID.randomUUID().toString()), actorCrn, accountId, machineUserCrn);
            LOGGER.info("Deleting access keys for {}", machineUserCrn);
            client.deleteAccessKeys(UUID.randomUUID().toString(), accessKeys, actorCrn);
        }
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
    ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }

    @VisibleForTesting
    UmsClient makeClient(ManagedChannel channel, String actorCrn) {
        return new UmsClient(channel, actorCrn, umsClientConfig);
    }

    /**
     * Queries the metadata file used to configure SSO authentication on clusters.
     *
     * @param accountId the account ID
     * @return metadata as string
     */
    public String getIdentityProviderMetadataXml(String accountId, String actorCrn) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            String requestId = UUID.randomUUID().toString();
            LOGGER.debug("Getting IdP metadata through account ID: {}, request id: {}", accountId, requestId);
            return client.getIdentityProviderMetadataXml(requestId, accountId);
        }
    }

    // Cache evict does not work with this key, we need to wait 60s
    @Caching(evict = {
            @CacheEvict(cacheNames = {"umsUserRoleAssigmentsCache"}, key = "#userCrn"),
            @CacheEvict(cacheNames = {"umsResourceAssigneesCache"}, key = "#userCrn"),
            @CacheEvict(cacheNames = {"umsUserRightsCache"}, key = "{ #userCrn, #right, #resource }")
    })
    public void assignResourceRole(
            String userCrn, String resourceCrn, String resourceRoleCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN);
            LOGGER.info("Assigning {} role for resource {} to user {}", resourceRoleCrn, resourceCrn, userCrn);
            client.assignResourceRole(requestId.orElse(UUID.randomUUID().toString()), userCrn, resourceCrn, resourceRoleCrn);
            LOGGER.info("Assigned {} role for resource {} to user {}", resourceRoleCrn, resourceCrn, userCrn);
        }
    }

    public void assignResourceOwnerRoleIfEntitled(String userCrn, String resourceCrn, String accountId) {
        try {
            if (isAuthorizationEntitlementRegistered(userCrn, accountId)) {
                assignResourceRole(userCrn, resourceCrn, getBuiltInOwnerResourceRoleCrn(), MDCUtils.getRequestId());
                LOGGER.debug("Owner role of {} is successfully assigned to the {} user", resourceCrn, userCrn);
            }
        } catch (StatusRuntimeException ex) {
            if (Status.Code.ALREADY_EXISTS.equals(ex.getStatus().getCode())) {
                LOGGER.debug("Owner role of {} is already assigned to the {} user", resourceCrn, userCrn);
            } else {
                throw ex;
            }
        }
    }

    // Cache evict does not work with this key, we need to wait 60s
    // @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void unassignResourceRole(String userCrn, String resourceCrn, String resourceRoleCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN);
            LOGGER.info("Unassigning {} role for resource {} from user {}", resourceRoleCrn, resourceCrn, userCrn);
            client.unassignResourceRole(requestId.orElse(UUID.randomUUID().toString()), userCrn, resourceCrn, resourceRoleCrn);
            LOGGER.info("Unassigned {} role for resource {} from user {}", resourceRoleCrn, resourceCrn, userCrn);
        }
    }

    // Cache evict does not work with this key, we need to wait 60s
    // @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void notifyResourceDeleted(String resourceCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            LOGGER.debug("Notify UMS about resource ('{}') was deleted", resourceCrn);
            UmsClient client = makeClient(channelWrapper.getChannel(), ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN);
            client.notifyResourceDeleted(requestId.orElse(UUID.randomUUID().toString()), resourceCrn);
            LOGGER.info("Notified UMS about deletion of resource {}", resourceCrn);
        } catch (Exception e) {
            LOGGER.error(String.format("Notifying UMS about deletion of resource %s has failed: ", resourceCrn), e);
        }
    }

    public String setWorkloadAdministrationGroupName(String actorCrn, String accountId, Optional<String> requestId, String right, String resource) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            return client.setWorkloadAdministrationGroupName(requestId.orElse(UUID.randomUUID().toString()),
                    accountId, right, resource).getWorkloadAdministrationGroupName();
        }
    }

    public String getWorkloadAdministrationGroupName(String actorCrn, String accountId, Optional<String> requestId, String right, String resource) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            return client.getWorkloadAdministrationGroupName(requestId.orElse(UUID.randomUUID().toString()),
                    accountId, right, resource).getWorkloadAdministrationGroupName();
        }
    }

    public void deleteWorkloadAdministrationGroupName(String actorCrn, String accountId, Optional<String> requestId, String right, String resource) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            client.deleteWorkloadAdministrationGroupName(requestId.orElse(UUID.randomUUID().toString()), accountId, right, resource);
        }
    }

    public List<UserManagementProto.WorkloadAdministrationGroup> listWorkloadAdministrationGroups(String actorCrn, String accountId,
            Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            return client.listWorkloadAdministrationGroups(requestId.orElse(UUID.randomUUID().toString()), accountId);
        }
    }

    /**
     * Retrieves event generation ids for an account from UMS.
     *
     * @param actorCrn  the CRN of the actor
     * @param accountId the account id
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    public GetEventGenerationIdsResponse getEventGenerationIds(String actorCrn, String accountId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting event generation ids for account {} using request ID {}", accountId, requestId);
            return client.getEventGenerationIds(requestId.orElse(UUID.randomUUID().toString()), accountId);
        }
    }

    /**
     * Get built-in Dbus uploader role
     * Partition and region is hard coded right now, if it will change use the same as the user crn
     */
    public String getBuiltInDatabusRoleCrn() {
        return getRoleCrn("DbusUploader").toString();
    }

    public String getBuiltInOwnerResourceRoleCrn() {
        return getResourceRoleCrn("Owner").toString();
    }

    public String getBuiltInEnvironmentAdminResourceRoleCrn() {
        return getResourceRoleCrn("EnvironmentAdmin").toString();
    }

    public Crn getResourceRoleCrn(String resourceRoleName) {
        return getBaseIamCrnBuilder()
                .setResourceType(Crn.ResourceType.RESOURCE_ROLE)
                .setResource(resourceRoleName)
                .build();
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

    public Crn getRoleCrn(String roleName) {
        return getBaseIamCrnBuilder()
                .setResourceType(Crn.ResourceType.ROLE)
                .setResource(roleName)
                .build();
    }

    private Crn.Builder getBaseIamCrnBuilder() {
        return Crn.builder()
                .setPartition(Crn.Partition.ALTUS)
                .setAccountId(ACCOUNT_IN_IAM_CRNS)
                .setService(Crn.Service.IAM);
    }

    private void setUmsConfig(UmsConfig umsConfig) {
        this.umsConfig = umsConfig;
    }

    private void setUmsClientConfig(UmsClientConfig umsClientConfig) {
        this.umsClientConfig = umsClientConfig;
    }
}

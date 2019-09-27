package com.sequenceiq.cloudbreak.auth.altus;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetRightsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;
import com.sequenceiq.cloudbreak.auth.altus.exception.UmsOperationException;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@Component
public class GrpcUmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    @Inject
    private UmsConfig umsConfig;

    @Inject
    private UmsClientConfig umsClientConfig;

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
            LOGGER.debug("{} Groups found for account {}", groups.size(), accountId);
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
    public MachineUser getMachineUserDetails(String actorCrn, String userCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting machine user information for {} using request ID {}", userCrn, requestId);
            MachineUser machineUser = client.getMachineUser(requestId.orElse(UUID.randomUUID().toString()), userCrn);
            LOGGER.debug("MachineUser information retrieved for userCrn: {}", machineUser.getCrn());
            return machineUser;
        }
    }

    /**
     * Retrieves list of all machine users from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    public List<MachineUser> listAllMachineUsers(String actorCrn, String accountId, Optional<String> requestId) {
        return listMachineUsers(actorCrn, accountId, null, requestId);
    }

    /**
     * Retrieves machine user list from UMS.
     *
     * @param accountId       the account Id
     * @param requestId       an optional request Id
     * @param machineUserCrns machine users to list. if null or empty then all machine users will be listed
     * @return the user associated with this user CRN
     */
    public List<MachineUser> listMachineUsers(String actorCrn, String accountId, List<String> machineUserCrns, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Listing machine user information for account {} using request ID {}", accountId, requestId);
            List<MachineUser> users = client.listMachineUsers(requestId.orElse(UUID.randomUUID().toString()), accountId, machineUserCrns);
            LOGGER.debug("{} Users found for account {}", users.size(), accountId);
            return users;
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
    public Optional<String> createMachineUser(String machineUserName, String userCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            String generatedRequestId = requestId.orElse(UUID.randomUUID().toString());
            LOGGER.debug("Creating machine user {} for {} using request ID {}", machineUserName, userCrn, generatedRequestId);
            Optional<String> machineUserCrn = client.createMachineUser(requestId.orElse(UUID.randomUUID().toString()), userCrn, machineUserName);
            if (machineUserCrn.isEmpty()) {
                MachineUser machineUser = client.getMachineUserForUser(requestId.orElse(UUID.randomUUID().toString()), userCrn, machineUserName);
                machineUserCrn = Optional.of(machineUser.getCrn());
            }
            LOGGER.debug("Machine User information retrieved for userCrn: {}", machineUserCrn.orElse(null));
            return machineUserCrn;
        } catch (StatusRuntimeException ex) {
            if (Status.NOT_FOUND.getCode().equals(ex.getStatus().getCode())) {
                String errMessage = String.format("Machine user with name %s is not found yet", machineUserName);
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
     * Retrieves account details from UMS, which includes the CM license.
     *
     * @param actorCrn  the CRN of the actor
     * @param userCrn   the CRN of the user
     * @param requestId an optional request Id
     * @return the account associated with this user CRN
     */
    @Cacheable(cacheNames = "umsAccountCache", key = "{ #actorCrn, #userCrn }")
    public Account getAccountDetails(String actorCrn, String userCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting account information for {} using request ID {}", userCrn, requestId);
            return client.getAccount(requestId.orElse(UUID.randomUUID().toString()), userCrn);
        }
    }

    @Cacheable(cacheNames = "umsUserRoleAssigmentsCache", key = "{ #actorCrn, #userCrn }")
    public List<UserManagementProto.ResourceAssignment> listResourceRoleAssigments(String actorCrn, String userCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            return client.listAssigmentsOfUser(requestId.orElse(UUID.randomUUID().toString()), userCrn);
        }
    }

    @Cacheable(cacheNames = "umsUserRightsCache", key = "{ #actorCrn, #userCrn, #right, #resource }")
    public boolean checkRight(String actorCrn, String userCrn, String right, String resource, Optional<String> requestId) {
        if (InternalCrnBuilder.isInternalCrn(actorCrn)) {
            LOGGER.info("InternalCrn, allow right {} for user {}!", right, userCrn);
            return true;
        }
        if (isReadRight(right)) {
            LOGGER.info("Letting read operation through for right {} for user {}!", right, userCrn);
            return true;
        }
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            AuthorizationClient client = new AuthorizationClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Checking right {} for user {}!", right, userCrn);
            client.checkRight(requestId.orElse(UUID.randomUUID().toString()), userCrn, right, null);
            LOGGER.info("User {} has right {}!", userCrn, right);
            return true;
        } catch (Exception e) {
            LOGGER.error("Checking right {} failed for user {}, thus access is denied! Cause: {}", right, userCrn, e.getMessage());
            return false;
        }
    }

    @Cacheable(cacheNames = "umsUserRightsCache", key = "{ #actorCrn, #userCrn, #right, #resource }")
    public boolean checkRight(String actorCrn, String userCrn, String right, Optional<String> requestId) {
        return checkRight(actorCrn, userCrn, right, null, requestId);
    }

    @Cacheable(cacheNames = "umsResourceAssigneesCache", key = "{ #actorCrn, #userCrn, #resourceCrn }")
    public List<UserManagementProto.ResourceAssignee> listAssigneesOfResource(String actorCrn, String userCrn,
            String resourceCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            return client.listResourceAssigneesForResource(requestId.orElse(UUID.randomUUID().toString()), resourceCrn);
        }
    }

    /**
     * Add role to machine user
     *
     * @param userCrn        actor that will assign the role
     * @param machineUserCrn machine user
     * @param roleCrn        role that will be assigned
     * @param requestId      id for the request
     */
    public void assignMachineUserRole(String userCrn, String machineUserCrn, String roleCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            client.assignMachineUserRole(requestId.orElse(UUID.randomUUID().toString()),
                    userCrn, machineUserCrn, roleCrn);
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
    public AltusCredential generateAccessSecretKeyPair(String actorCrn, String machineUserCrn,
            Optional<String> requestId, UserManagementProto.AccessKeyType.Value accessKeyType) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Generating new access / secret key pair for {}", machineUserCrn);
            CreateAccessKeyResponse accessKeyResponse = client.createAccessPrivateKeyPair(
                    requestId.orElse(UUID.randomUUID().toString()), actorCrn, machineUserCrn, accessKeyType);
            return new AltusCredential(accessKeyResponse.getAccessKey().getAccessKeyId(), accessKeyResponse.getPrivateKey().toCharArray());
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
    public AltusCredential createMachineUserAndGenerateKeys(String machineUserName, String userCrn, String roleCrn) {
        return createMachineUserAndGenerateKeys(machineUserName, userCrn, roleCrn, UserManagementProto.AccessKeyType.Value.UNSET);
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
    public AltusCredential createMachineUserAndGenerateKeys(String machineUserName, String userCrn,
            String roleCrn, UserManagementProto.AccessKeyType.Value accessKeyType) {
        Optional<String> machineUserCrn = createMachineUser(machineUserName, userCrn, Optional.empty());
        if (StringUtils.isNotEmpty(roleCrn)) {
            assignMachineUserRole(userCrn, machineUserCrn.orElse(null), roleCrn, Optional.empty());
        }
        return generateAccessSecretKeyPair(userCrn, machineUserCrn.orElse(null), Optional.empty(), accessKeyType);
    }

    /**
     * Cleanup machine user related resources (access keys, role, user)
     *
     * @param machineUserName machine user name
     * @param userCrn         crn of the actor
     * @param roleCrn         crn of the role
     */
    public void clearMachineUserWithAccessKeysAndRole(String machineUserName, String userCrn, String roleCrn) {
        if (StringUtils.isNotEmpty(roleCrn)) {
            unassignMachineUserRole(userCrn, machineUserName, roleCrn, Optional.empty());
        }
        deleteMachineUserAccessKeys(userCrn, machineUserName, Optional.empty());
        deleteMachineUser(machineUserName, userCrn, Optional.empty());
    }

    /**
     * Delete all access key for machine user
     *
     * @param actorCrn       actor that executes the deletions
     * @param machineUserCrn machine user
     * @param requestId      id for the request
     */
    public void deleteMachineUserAccessKeys(String actorCrn, String machineUserCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Getting access keys for {}", machineUserCrn);
            List<String> accessKeys = client.listMachineUserAccessKeys(requestId.orElse(UUID.randomUUID().toString()), actorCrn, machineUserCrn);
            LOGGER.info("Deleting access keys for {}", machineUserCrn);
            client.deleteAccessKeys(UUID.randomUUID().toString(), accessKeys, actorCrn);
        }
    }

    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }

    private UmsClient makeClient(ManagedChannel channel, String accountId) {
        return new UmsClient(channel, accountId, umsClientConfig);
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
    // @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void assignResourceRole(String userCrn, String resourceCrn, String resourceRoleCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            LOGGER.info("Assigning {} role for resource {} to user {}", resourceRoleCrn, resourceCrn, userCrn);
            client.assignResourceRole(requestId.orElse(UUID.randomUUID().toString()), userCrn, resourceCrn, resourceRoleCrn);
            LOGGER.info("Assigned {} role for resource {} to user {}", resourceRoleCrn, resourceCrn, userCrn);
        }
    }

    // Cache evict does not work with this key, we need to wait 60s
    // @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void unassignResourceRole(String userCrn, String resourceCrn, String resourceRoleCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            LOGGER.info("Unassigning {} role for resource {} from user {}", resourceRoleCrn, resourceCrn, userCrn);
            client.unassignResourceRole(requestId.orElse(UUID.randomUUID().toString()), userCrn, resourceCrn, resourceRoleCrn);
            LOGGER.info("Unassigned {} role for resource {} from user {}", resourceRoleCrn, resourceCrn, userCrn);
        }
    }

    // Cache evict does not work with this key, we need to wait 60s
    // @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void notifyResourceDeleted(String userCrn, String resourceCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            client.notifyResourceDeleted(requestId.orElse(UUID.randomUUID().toString()), resourceCrn);
        }
    }

    @Cacheable(cacheNames = "umsRolesCache", key = "{ #accountId }")
    public List<UserManagementProto.Role> listRoles(String actorCrn, String accountId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            return client.listRoles(requestId.orElse(UUID.randomUUID().toString()), accountId).getRoleList();
        }
    }

    /**
     * Get built-in Dbus uploader role
     * Partition and region is hard coded right now, if it will change use the same as the user crn
     */
    public String getBuiltInDatabusRoleCrn() {
        Crn databusCrn = Crn.builder()
                .setPartition(Crn.Partition.ALTUS)
                .setAccountId("altus")
                .setService(Crn.Service.IAM)
                .setResourceType(Crn.ResourceType.ROLE)
                .setResource("DbusUploader")
                .build();
        return databusCrn.toString();
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
}

package com.sequenceiq.cloudbreak.auth.altus;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.CreateAccessKeyResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Component
public class GrpcUmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    @Inject
    private UmsConfig umsConfig;

    @Inject
    private UmsClientConfig umsClientConfig;

    /**
     * Retrieves user details from UMS.
     *
     * @param actorCrn  the CRN of the actor
     * @param userCrn   the CRN of the user
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    @Cacheable(cacheNames = "umsUserCache")
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
     * Retrieves user list from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    public List<User> listUsers(String actorCrn, String accountId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Listing user information for account {} using request ID {}", accountId, requestId);
            List<User> users = client.listUsers(requestId.orElse(UUID.randomUUID().toString()), accountId);
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
    @Cacheable(cacheNames = "umsMachineUserCache")
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
     * Retrieves machine user list from UMS.
     *
     * @param accountId the account Id
     * @param requestId an optional request Id
     * @return the user associated with this user CRN
     */
    public List<MachineUser> listMachineUsers(String actorCrn, String accountId, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Listing machine user information for account {} using request ID {}", accountId, requestId);
            List<MachineUser> users = client.listMachineUsers(requestId.orElse(UUID.randomUUID().toString()), accountId);
            LOGGER.debug("{} Users found for account {}", users.size(), accountId);
            return users;
        }
    }

    /**
     * Creates new machine user
     *
     * @param machineUserName   new machine user name
     * @param userCrn           the CRN of the user
     * @param requestId         an optional request Id
     * @return the user associated with this user CRN
     */
    public MachineUser createMachineUser(String machineUserName, String userCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            String generatedRequestId = requestId.orElse(UUID.randomUUID().toString());
            LOGGER.debug("Creating machine user {} for {} using request ID {}", machineUserName, userCrn, generatedRequestId);
            client.createMachineUser(requestId.orElse(UUID.randomUUID().toString()), userCrn, machineUserName);
            MachineUser machineUser = client.getMachineUserForUser(requestId.orElse(UUID.randomUUID().toString()), userCrn, machineUserName);
            LOGGER.debug("Machine User information retrieved for userCrn: {}", machineUser.getCrn());
            return machineUser;
        }
    }

    /**
     * Delete machine user
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

    public List<String> getGroupsForUser(String actorCrn, String userCrn, String environmentCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Listing groups for user {} in environment {}", userCrn, environmentCrn);
            List<String> groups = client.getGroupsForUser(requestId.orElse(UUID.randomUUID().toString()), userCrn, environmentCrn);
            LOGGER.debug("Found groups {}", groups);
            return groups;
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
    @Cacheable(cacheNames = "umsAccountCache")
    public Account getAccountDetails(String actorCrn, String userCrn, Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting account information for {} using request ID {}", userCrn, requestId);
            return client.getAccount(requestId.orElse(UUID.randomUUID().toString()), userCrn);
        }
    }

    @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void assignResourceRole(String userCrn, String resourceCrn, String resourceRoleCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            LOGGER.info("Assigning {} role for resource {} to user {}", resourceRoleCrn, resourceCrn, userCrn);
            client.assignResourceRole(requestId, userCrn, resourceCrn, resourceRoleCrn);
            LOGGER.info("Assigned {} role for resource {} to user {}", resourceRoleCrn, resourceCrn, userCrn);
        }
    }

    @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void unassignResourceRole(String userCrn, String resourceCrn, String resourceRoleCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            LOGGER.info("Unassigning {} role for resource {} from user {}", resourceRoleCrn, resourceCrn, userCrn);
            client.unassignResourceRole(requestId, userCrn, resourceCrn, resourceRoleCrn);
            LOGGER.info("Unassigned {} role for resource {} from user {}", resourceRoleCrn, resourceCrn, userCrn);
        }
    }

    @Cacheable(cacheNames = "umsUserRoleAssigmentsCache")
    public List<UserManagementProto.ResourceAssignment> listResourceRoleAssigments(String userCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            return client.listAssigmentsOfUser(requestId, userCrn);
        }
    }

    @Cacheable(cacheNames = "umsUserRightsCache")
    public boolean checkRight(String userCrn, String right, String resource, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            AuthorizationClient client = new AuthorizationClient(channelWrapper.getChannel(), userCrn);
            LOGGER.info("Checking right {} for user {}!", right, userCrn);
            client.checkRight(requestId, userCrn, right, null);
            LOGGER.info("User {} has right {}!", userCrn, right);
            return true;
        } catch (Exception e) {
            LOGGER.error("Checking right {} failed for user {}, thus access is denied! Cause: {}", right, userCrn, e.getMessage());
            return false;
        }
    }

    @Cacheable(cacheNames = "umsResourceAssigneesCache")
    public List<UserManagementProto.ResourceAssignee> listAssigneesOfResource(String userCrn, String resourceCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            return client.listResourceAssigneesForResource(requestId, resourceCrn);
        }
    }

    @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void notifyResourceDeleted(String userCrn, String resourceCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), userCrn);
            client.notifyResourceDeleted(requestId, resourceCrn);
        }
    }

    /**
     * Add role to machine user
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
     * @param userCrn actor that will unassign the role
     * @param machineUserCrn machine user
     * @param roleCrn role that will be removed
     *
     * @param requestId id for the request
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
     * @param actorCrn actor that executes the key generation
     * @param machineUserCrn machine user (owner of the access key)
     * @param requestId id for the request
     * @return access / private key holder object
     */
    public AltusCredential generateAccessSecretKeyPair(String actorCrn, String machineUserCrn,
            Optional<String> requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = makeClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.info("Generating new access / secret key pair for {}", machineUserCrn);
            CreateAccessKeyResponse accessKeyResponse = client.createAccessPrivateKeyPair(
                    requestId.orElse(UUID.randomUUID().toString()), actorCrn, machineUserCrn);
            return new AltusCredential(accessKeyResponse.getAccessKey().getAccessKeyId(), accessKeyResponse.getPrivateKey().toCharArray());
        }
    }

    /**
     * Delete all access key for machine user
     * @param actorCrn actor that executes the deletions
     * @param machineUserCrn machine user
     * @param requestId id for the request
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
}

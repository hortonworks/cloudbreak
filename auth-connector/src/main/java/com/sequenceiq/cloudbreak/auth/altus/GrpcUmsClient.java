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
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.MachineUser;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;

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
            LOGGER.debug("Getting machineuser information for {} using request ID {}", userCrn, requestId);
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

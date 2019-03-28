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
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;

import io.grpc.ManagedChannelBuilder;

@Component
public class GrpcUmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    @Inject
    private UmsConfig umsConfig;

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
            UmsClient client = new UmsClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting user information for {} using request ID {}", userCrn, requestId);
            User user = client.getUser(requestId.orElse(UUID.randomUUID().toString()), userCrn);
            LOGGER.debug("User information retrieved for userCrn: {}", user.getCrn());
            return user;
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
            UmsClient client = new UmsClient(channelWrapper.getChannel(), actorCrn);
            LOGGER.debug("Getting account information for {} using request ID {}", userCrn, requestId);
            return client.getAccount(requestId.orElse(UUID.randomUUID().toString()), userCrn);
        }
    }

    @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void assignResourceRole(String userCrn, String resourceCrn, String resourceRoleCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), userCrn);
            LOGGER.info("Assigning {} role for resource {} to user {}", resourceRoleCrn, resourceCrn, userCrn);
            client.assignResourceRole(requestId, userCrn, resourceCrn, resourceRoleCrn);
            LOGGER.info("Assigned {} role for resource {} to user {}", resourceRoleCrn, resourceCrn, userCrn);
        }
    }

    @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void unassignResourceRole(String userCrn, String resourceCrn, String resourceRoleCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), userCrn);
            LOGGER.info("Unassigning {} role for resource {} from user {}", resourceRoleCrn, resourceCrn, userCrn);
            client.unassignResourceRole(requestId, userCrn, resourceCrn, resourceRoleCrn);
            LOGGER.info("Unassigned {} role for resource {} from user {}", resourceRoleCrn, resourceCrn, userCrn);
        }
    }

    @Cacheable(cacheNames = "umsUserRoleAssigmentsCache")
    public List<UserManagementProto.ResourceAssignment> listResourceRoleAssigments(String userCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), userCrn);
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
            UmsClient client = new UmsClient(channelWrapper.getChannel(), userCrn);
            return client.listResourceAssigneesForResource(requestId, resourceCrn);
        }
    }

    @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void notifyResourceDeleted(String userCrn, String resourceCrn, String requestId) {
        try (ManagedChannelWrapper channelWrapper = makeWrapper()) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), userCrn);
            client.notifyResourceDeleted(requestId, resourceCrn);
        }
    }

    public boolean isConfigured() {
        return umsConfig.isConfigured();
    }

    public boolean isUmsUsable(String crn) {
        return umsConfig.isConfigured() && Crn.isCrn(crn);
    }

    private ManagedChannelWrapper makeWrapper() {
        return new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build());
    }
}

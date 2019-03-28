package com.sequenceiq.cloudbreak.auth.altus;


import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;

import io.grpc.ManagedChannelBuilder;

@Component
public class GrpcUmsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcUmsClient.class);

    @Inject
    private UmsConfig umsConfig;

    private static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    @Cacheable(cacheNames = "umsUserCache")
    public UserManagementProto.User getUserDetails(String actorCrn, String userCrn) {
        try (ManagedChannelWrapper channelWrapper = new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build())) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), actorCrn);
            String requestId = newRequestId();
            LOGGER.info("Getting user information for {} using request ID {}", userCrn, requestId);
            UserManagementProto.User user = client.getUser(requestId, userCrn);
            LOGGER.info("User information retrieved for userCrn: {}", user.getCrn());
            return user;
        }
    }

    @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void assignResourceRole(String userCrn, String resourceCrn, String resourceRoleCrn) {
        try (ManagedChannelWrapper channelWrapper = new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build())) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), userCrn);
            LOGGER.info("Assigning {} role for resource {} to user {}", resourceRoleCrn, resourceCrn, userCrn);
            String requestId = newRequestId();
            client.assignResourceRole(requestId, userCrn, resourceCrn, resourceRoleCrn);
            LOGGER.info("Assigned {} role for resource {} to user {}", resourceRoleCrn, resourceCrn, userCrn);
        }
    }

    @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void unassignResourceRole(String userCrn, String resourceCrn, String resourceRoleCrn) {
        try (ManagedChannelWrapper channelWrapper = new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build())) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), userCrn);
            LOGGER.info("Unassigning {} role for resource {} from user {}", resourceRoleCrn, resourceCrn, userCrn);
            String requestId = newRequestId();
            client.unassignResourceRole(requestId, userCrn, resourceCrn, resourceRoleCrn);
            LOGGER.info("Unassigned {} role for resource {} from user {}", resourceRoleCrn, resourceCrn, userCrn);
        }
    }

    @Cacheable(cacheNames = "umsUserRoleAssigmentsCache")
    public List<UserManagementProto.ResourceAssignment> listResourceRoleAssigments(String userCrn) {
        try (ManagedChannelWrapper channelWrapper = new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build())) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), userCrn);
            String requestId = newRequestId();
            return client.listAssigmentsOfUser(requestId, userCrn);
        }
    }

    @Cacheable(cacheNames = "umsUserRightsCache")
    public boolean checkRight(String userCrn, String right, String resource) {
        try (ManagedChannelWrapper channelWrapper = new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build())) {
            AuthorizationClient client = new AuthorizationClient(channelWrapper.getChannel(), userCrn);
            LOGGER.info("Checking right {} for resource {} for user {}!", right, resource, userCrn);
            String requestId = newRequestId();
            client.checkRight(requestId, userCrn, right, resource);
            LOGGER.info("User {} has right {} for resource {}!", userCrn, right, resource);
            return true;
        } catch (Exception e) {
            LOGGER.error("Checking right {} for resource {} failed for user {}, thus access is denied! Cause: {}", right, resource, userCrn, e.getMessage());
            return false;
        }
    }

    @Cacheable(cacheNames = "umsResourceAssigneesCache")
    public List<UserManagementProto.ResourceAssignee> listAssigneesOfResource(String userCrn, String resourceCrn) {
        try (ManagedChannelWrapper channelWrapper = new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build())) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), userCrn);
            String requestId = newRequestId();
            return client.listResourceAssigneesForResource(requestId, resourceCrn);
        }
    }

    @CacheEvict(cacheNames = {"umsUserRightsCache", "umsUserRoleAssigmentsCache", "umsResourceAssigneesCache"}, key = "#userCrn")
    public void notifyResourceDeleted(String userCrn, String resourceCrn) {
        try (ManagedChannelWrapper channelWrapper = new ManagedChannelWrapper(
                ManagedChannelBuilder.forAddress(umsConfig.getEndpoint(), umsConfig.getPort())
                        .usePlaintext()
                        .maxInboundMessageSize(DEFAULT_MAX_MESSAGE_SIZE)
                        .build())) {
            UmsClient client = new UmsClient(channelWrapper.getChannel(), userCrn);
            String requestId = newRequestId();
            client.notifyResourceDeleted(requestId, resourceCrn);
        }
    }

    public boolean isConfigured() {
        return umsConfig.isConfigured();
    }

    public boolean isUmsUsable(String crn) {
        return umsConfig.isConfigured() && Crn.isCrn(crn);
    }
}

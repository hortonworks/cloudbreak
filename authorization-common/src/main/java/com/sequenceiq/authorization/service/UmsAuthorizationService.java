package com.sequenceiq.authorization.service;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.ResourceType;
import com.sequenceiq.authorization.resource.RightUtils;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class UmsAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    // ACCOUNT LEVEL

    public void checkRightOfUserForResource(String userCrn, ResourceType resource, ResourceAction action, String unauthorizedMessage) {
        if (!hasRightOfUserForResource(userCrn, resource, action)) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    public void checkRightOfUserForResource(String userCrn, ResourceType resource, ResourceAction action) {
        String right = RightUtils.getRight(resource, action);
        String unauthorizedMessage = String.format("You have no right to perform %s. This requires one of these roles: %s. "
                        + "You can request access through IAM service from an administrator.",
                right, "PowerUser");
        checkRightOfUserForResource(userCrn, resource, action, unauthorizedMessage);
    }

    public Boolean hasRightOfUserForResource(String userCrn, String resource, String action) {
        Optional<ResourceType> resourceEnum = ResourceType.getByName(resource);
        Optional<ResourceAction> actionEnum = ResourceAction.getByName(action);
        if (!resourceEnum.isPresent() || !actionEnum.isPresent()) {
            throw new BadRequestException("Resource or action cannot be found by request!");
        }
        if (!hasRightOfUserForResource(userCrn, resourceEnum.get(), actionEnum.get())) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public boolean hasRightOfUserForResource(String userCrn, ResourceType resource, ResourceAction action) {
        return umsClient.checkRight(userCrn, userCrn, RightUtils.getRight(resource, action), getRequestId());
    }

    // RESOURCE LEVEL

    public void checkRightOfUserForResourceOnResource(String userCrn, ResourceType resource, ResourceAction action, String resourceCrn) {
        String right = RightUtils.getRight(resource, action);
        String unauthorizedMessage = String.format("You have no right to perform %s. This requires one of these roles: %s. "
                        + "You can request access through IAM service from an administrator.",
                right, "PowerUser");
        checkRightOfUserForResourceOnResource(userCrn, resource, action, resourceCrn, unauthorizedMessage);
    }

    public void checkRightOfUserForResourceOnResource(String userCrn, ResourceType resource, ResourceAction action,
            String resourceCrn, String unauthorizedMessage) {
        if (!umsClient.checkRight(userCrn, userCrn, RightUtils.getRight(resource, action), resourceCrn, getRequestId())) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    protected Optional<String> getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return Optional.of(requestId);
    }
}

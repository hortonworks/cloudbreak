package com.sequenceiq.authorization.service;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.RightUtils;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class UmsAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    public void checkRightOfUserForResource(String userCrn, AuthorizationResource resource, ResourceAction action, String unauthorizedMessage) {
        if (!umsClient.checkRight(userCrn, userCrn, RightUtils.getRight(resource, action), getRequestId())) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    public boolean hasRightOfUserForResource(String userCrn, AuthorizationResource resource, ResourceAction action) {
        return umsClient.checkRight(userCrn, userCrn, RightUtils.getRight(resource, action), getRequestId());
    }

    public void checkRightOfUserForResource(String userCrn, AuthorizationResource resource, ResourceAction action) {
        String unauthorizedMessage = String.format("You have no right to perform %s on %s. This requires PowerUser role. "
                        + "You can request access through IAM service from an administrator.",
                RightUtils.getRight(resource, action), resource.name());
        checkRightOfUserForResource(userCrn, resource, action, unauthorizedMessage);
    }

    protected Optional<String> getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return Optional.of(requestId);
    }
}

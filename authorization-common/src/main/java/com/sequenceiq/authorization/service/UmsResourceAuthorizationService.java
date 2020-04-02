package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.RightUtils;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class UmsResourceAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsResourceAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    public void checkRightOfUserOnResource(String userCrn, AuthorizationResourceType resource,
            AuthorizationResourceAction action, String resourceCrn) {
        String right = RightUtils.getRight(resource, action);
        String unauthorizedMessage = String.format("You have no right to perform %s on resource %s.", right, resourceCrn);
        checkRightOfUserOnResource(userCrn, right, resourceCrn, unauthorizedMessage);
    }

    public Map<String, Boolean> getRightOfUserOnResources(String userCrn, AuthorizationResourceType resource, AuthorizationResourceAction action,
            List<String> resourceCrns) {
        return umsClient.hasRights(userCrn, userCrn, resourceCrns, RightUtils.getRight(resource, action), getRequestId());
    }

    public void checkRightOfUserOnResources(String userCrn, AuthorizationResourceType resource,
            AuthorizationResourceAction action, Collection<String> resourceCrns) {
        String right = RightUtils.getRight(resource, action);
        String unauthorizedMessage = String.format("You have no right to perform %s on resources [%s]", right, Joiner.on(",").join(resourceCrns));
        checkRightOfUserOnResources(userCrn, right, resourceCrns, unauthorizedMessage);
    }

    private void checkRightOfUserOnResource(String userCrn, String right, String resourceCrn, String unauthorizedMessage) {
        if (!umsClient.checkRight(userCrn, userCrn, right, resourceCrn, getRequestId())) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    private void checkRightOfUserOnResources(String userCrn, String right, Collection<String> resourceCrns, String unauthorizedMessage) {
        if (!umsClient.hasRights(userCrn, userCrn, Lists.newArrayList(resourceCrns), right, getRequestId())
                .values().stream().allMatch(Boolean::booleanValue)) {
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

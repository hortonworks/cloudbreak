package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.RightUtils;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class UmsAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    public void checkRightOfUser(String userCrn, AuthorizationResourceType resourceType, AuthorizationResourceAction action) {
        String right = RightUtils.getRight(resourceType, action);
        String unauthorizedMessage = String.format("You have no right to perform %s in account %s.", right, Crn.fromString(userCrn).getAccountId());
        checkRightOfUser(userCrn, resourceType, action, unauthorizedMessage);
    }

    public Boolean hasRightOfUser(String userCrn, String resourceType, String action) {
        Optional<AuthorizationResourceType> resourceEnum = AuthorizationResourceType.getByName(resourceType);
        Optional<AuthorizationResourceAction> actionEnum = AuthorizationResourceAction.getByName(action);
        if (!resourceEnum.isPresent() || !actionEnum.isPresent()) {
            throw new BadRequestException("Resource or action cannot be found by request!");
        }
        if (!hasRightOfUser(userCrn, resourceEnum.get(), actionEnum.get())) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private void checkRightOfUser(String userCrn, AuthorizationResourceType resourceType, AuthorizationResourceAction action, String unauthorizedMessage) {
        if (!hasRightOfUser(userCrn, resourceType, action)) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    private boolean hasRightOfUser(String userCrn, AuthorizationResourceType resourceType, AuthorizationResourceAction action) {
        return umsClient.checkRight(userCrn, userCrn, RightUtils.getRight(resourceType, action), getRequestId());
    }

    public void checkRightOfUserOnResource(String userCrn, AuthorizationResourceType resource,
            AuthorizationResourceAction action, String resourceCrn) {
        String right = RightUtils.getRight(resource, action);
        String unauthorizedMessage = String.format("You have no right to perform %s on resource %s.", right, resourceCrn);
        checkRightOfUserOnResource(userCrn, resource, action, resourceCrn, unauthorizedMessage);
    }

    public void checkRightOfUserOnResources(String userCrn, AuthorizationResourceType resource,
            AuthorizationResourceAction action, Collection<String> resourceCrns) {
        String right = RightUtils.getRight(resource, action);
        String unauthorizedMessage = String.format("You have no right to perform %s on resources [%s]", right, Joiner.on(",").join(resourceCrns));
        checkRightOfUserOnResources(userCrn, resource, action, resourceCrns, unauthorizedMessage);
    }

    private void checkRightOfUserOnResource(String userCrn, AuthorizationResourceType resource, AuthorizationResourceAction action,
            String resourceCrn, String unauthorizedMessage) {
        if (!umsClient.checkRight(userCrn, userCrn, RightUtils.getRight(resource, action), resourceCrn, getRequestId())) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    private void checkRightOfUserOnResources(String userCrn, AuthorizationResourceType resource, AuthorizationResourceAction action,
            Collection<String> resourceCrns, String unauthorizedMessage) {
        Map<String, String> resourceCrnRightMap = resourceCrns.stream().distinct().collect(
                Collectors.toMap(resourceCrn -> resourceCrn, resourceCrn -> RightUtils.getRight(resource, action)));
        if (!umsClient.hasRights(userCrn, userCrn, resourceCrnRightMap, getRequestId()).stream().allMatch(Boolean::booleanValue)) {
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

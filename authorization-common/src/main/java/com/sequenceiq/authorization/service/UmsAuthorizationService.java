package com.sequenceiq.authorization.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
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
        String right = RightUtils.getRight(resource, action);
        String unauthorizedMessage = String.format("You have no right to perform %s. This requires one of these roles: %s. "
                        + "You can request access through IAM service from an administrator.",
                right, "PowerUser");
        checkRightOfUserForResource(userCrn, resource, action, unauthorizedMessage);
    }

    public Boolean hasRightOfUserForResource(String userCrn, String resource, String action) {
        Optional<AuthorizationResource> resourceEnum = AuthorizationResource.getByName(resource);
        Optional<ResourceAction> actionEnum = ResourceAction.getByName(action);
        if (!resourceEnum.isPresent() || !actionEnum.isPresent()) {
            throw new BadRequestException("Resource or action cannot be found by request!");
        }
        if (!hasRightOfUserForResource(userCrn, resourceEnum.get(), actionEnum.get())) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    protected Optional<String> getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return Optional.of(requestId);
    }

    private Map<String, String> getRolesByRights(String actorCrn) {
        List<UserManagementProto.Role> roles = umsClient.listRoles(actorCrn, Crn.fromString(actorCrn).getAccountId(), getRequestId());
        Map<String, String> rolesMap = Maps.newHashMap();
        Arrays.stream(AuthorizationResource.values()).forEach(authorizationResource -> {
            Arrays.stream(ResourceAction.values()).forEach(action -> {
                String right = RightUtils.getRight(authorizationResource, action);
                List<String> requiredRolesForRight = roles.stream()
                        .filter(role -> role.getPolicyList().stream()
                                .filter(policy -> policy.getPolicyDefinition().getStatementList().stream()
                                        .filter(policyStatement -> policyStatement.getRightList().stream()
                                                .filter(roleRight -> StringUtils.equals(roleRight, right))
                                                .findAny()
                                                .isPresent())
                                        .findAny()
                                        .isPresent())
                                .findAny()
                                .isPresent())
                        .map(role -> Crn.fromString(role.getCrn()).getResource())
                        .collect(Collectors.toList());
                String requiredRoles = requiredRolesForRight.isEmpty() ? "PowerUser" : Joiner.on(", ").join(requiredRolesForRight);
                rolesMap.put(right, requiredRoles);
            });
        });
        return rolesMap;
    }
}

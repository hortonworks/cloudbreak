package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class UmsResourceAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsResourceAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private UmsRightProvider umsRightProvider;

    public void checkRightOfUserOnResource(String userCrn, AuthorizationResourceAction action, String resourceCrn) {
        String right = umsRightProvider.getRight(action);
        String unauthorizedMessage = String.format("You have no right to perform %s on resource %s.", right, resourceCrn);
        checkRightOfUserOnResource(userCrn, right, resourceCrn, unauthorizedMessage);
    }

    public Map<String, Boolean> getRightOfUserOnResources(String userCrn, AuthorizationResourceAction action, List<String> resourceCrns) {
        return umsClient.hasRights(userCrn, userCrn, resourceCrns, umsRightProvider.getRight(action), getRequestId());
    }

    public void checkRightOfUserOnResources(String userCrn, AuthorizationResourceAction action, Collection<String> resourceCrns) {
        if (!umsClient.isAuthorizationEntitlementRegistered(userCrn, ThreadBasedUserCrnProvider.getAccountId())) {
            checkRightOfUserOnResource(userCrn, action, null);
            return;
        }
        String right = umsRightProvider.getRight(action);
        String unauthorizedMessage = String.format("You have no right to perform %s on resources [%s]", right, Joiner.on(",").join(resourceCrns));
        checkRightOfUserOnResources(userCrn, right, resourceCrns, unauthorizedMessage);
    }

    public void checkIfUserHasAtLeastOneRight(String userCrn, Map<String, AuthorizationResourceAction> checkedRightsForResources) {
        LOGGER.info("Check if user has at least one rigth: {}", checkedRightsForResources);
        List<AuthorizationProto.RightCheck> rightCheckList = checkedRightsForResources.entrySet().stream().map(entry ->
                AuthorizationProto.RightCheck.newBuilder()
                .setResource(entry.getKey())
                .setRight(umsRightProvider.getRight(entry.getValue()))
                .build()).collect(Collectors.toList());
        List<Boolean> rightCheckResults = umsClient.hasRights(userCrn, userCrn, rightCheckList, getRequestId());
        LOGGER.info("Right check results: {}", rightCheckResults);
        if (rightCheckResults.stream().noneMatch(Boolean::booleanValue)) {
            throw new AccessDeniedException("You have no right to perform the action");
        }
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

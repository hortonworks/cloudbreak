package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.utils.AuthorizationMessageUtils;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

@Service
public class UmsResourceAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsResourceAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private EntitlementService entitlementService;

    public void checkRightOfUserOnResource(String userCrn, AuthorizationResourceAction action, String resourceCrn) {
        String right = umsRightProvider.getRight(action);
        checkRightOfUserOnResource(userCrn, right, resourceCrn, AuthorizationMessageUtils.formatTemplate(right, resourceCrn));
    }

    public Map<String, Boolean> getRightOfUserOnResources(String userCrn, AuthorizationResourceAction action, List<String> resourceCrns) {
        return umsClient.hasRights(userCrn, userCrn, resourceCrns, umsRightProvider.getRight(action), getRequestId());
    }

    public void checkRightOfUserOnResources(String userCrn, AuthorizationResourceAction action, Collection<String> resourceCrns) {
        if (!entitlementService.isAuthorizationEntitlementRegistered(userCrn, ThreadBasedUserCrnProvider.getAccountId())) {
            checkRightOfUserOnResource(userCrn, action, null);
            return;
        }
        String right = umsRightProvider.getRight(action);
        checkRightOfUserOnResources(userCrn, right, resourceCrns, AuthorizationMessageUtils.formatTemplate(right, resourceCrns));
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
            throw new AccessDeniedException(AuthorizationMessageUtils.formatTemplate(rightCheckList));
        }
    }

    private void checkRightOfUserOnResource(String userCrn, String right, String resourceCrn, String unauthorizedMessage) {
        if (entitlementService.isAuthorizationEntitlementRegistered(userCrn, ThreadBasedUserCrnProvider.getAccountId())) {
            if (!umsClient.checkResourceRight(userCrn, userCrn, right, resourceCrn, getRequestId())) {
                LOGGER.error(unauthorizedMessage);
                throw new AccessDeniedException(unauthorizedMessage);
            }
        }
        if (!umsClient.checkResourceRightLegacy(userCrn, userCrn, right, resourceCrn, getRequestId())) {
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

    public boolean isEntitled(String actorCrn, String accountId, Entitlement entitlement) {
        boolean entitled;
        if (Entitlement.CB_AUTHZ_POWER_USERS.equals(entitlement) && StringUtils.equals(accountId, InternalCrnBuilder.INTERNAL_ACCOUNT)) {
            entitled = false;
        } else {
            return umsClient.getAccountDetails(actorCrn, accountId, MDCUtils.getRequestId()).getEntitlementsList()
                    .stream()
                    .map(e -> e.getEntitlementName().toUpperCase())
                    .anyMatch(e -> e.equalsIgnoreCase(entitlement.name()));
        }
        return entitled;
    }

    private boolean isEntitledAndLogResult(String actorCrn, String accountId, Entitlement entitlement) {
        boolean entitled = isEntitled(actorCrn, accountId, entitlement);
        LOGGER.debug("Entitlement result {}={}", entitlement, entitled);
        return entitled;
    }
}

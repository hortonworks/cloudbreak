package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.utils.AuthorizationMessageUtilsService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;
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

    @Inject
    private AuthorizationMessageUtilsService authorizationMessageUtilsService;

    public void checkRightOfUserOnResource(String userCrn, AuthorizationResourceAction action, String resourceCrn) {
        String right = umsRightProvider.getRight(action);
        checkRightOfUserOnResource(userCrn, right, resourceCrn, authorizationMessageUtilsService.formatTemplate(right, resourceCrn));
    }

    public Map<String, Boolean> getRightOfUserOnResources(String userCrn, AuthorizationResourceAction action, List<String> resourceCrns) {
        return umsClient.hasRights(userCrn, resourceCrns, umsRightProvider.getRight(action), getRequestId());
    }

    public void checkRightOfUserOnResources(String userCrn, AuthorizationResourceAction action, Collection<String> resourceCrns) {
        if (!entitlementService.isAuthorizationEntitlementRegistered(ThreadBasedUserCrnProvider.getAccountId())) {
            checkRightOfUserOnResource(userCrn, action, null);
            return;
        }
        String right = umsRightProvider.getRight(action);
        checkRightOfUserOnResources(userCrn, right, resourceCrns, authorizationMessageUtilsService.formatTemplate(right, resourceCrns));
    }

    private void checkRightOfUserOnResource(String userCrn, String right, String resourceCrn, String unauthorizedMessage) {
        if (entitlementService.isAuthorizationEntitlementRegistered(ThreadBasedUserCrnProvider.getAccountId())) {
            if (!umsClient.checkResourceRight(userCrn, right, resourceCrn, getRequestId())) {
                LOGGER.error(unauthorizedMessage);
                throw new AccessDeniedException(unauthorizedMessage);
            }
        }
        if (!umsClient.checkResourceRightLegacy(userCrn, right, resourceCrn, getRequestId())) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    private void checkRightOfUserOnResources(String userCrn, String right, Collection<String> resourceCrns, String unauthorizedMessage) {
        if (!umsClient.hasRights(userCrn, Lists.newArrayList(resourceCrns), right, getRequestId())
                .values().stream().allMatch(Boolean::booleanValue)) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    protected Optional<String> getRequestId() {
        return Optional.of(MDCBuilder.getOrGenerateRequestId());
    }

    public boolean isEntitled(String accountId, Entitlement entitlement) {
        boolean entitled;
        if (Entitlement.CB_AUTHZ_POWER_USERS.equals(entitlement) && StringUtils.equals(accountId, InternalCrnBuilder.INTERNAL_ACCOUNT)) {
            entitled = false;
        } else {
            return umsClient.getAccountDetails(accountId, MDCUtils.getRequestId()).getEntitlementsList()
                    .stream()
                    .map(e -> e.getEntitlementName().toUpperCase())
                    .anyMatch(e -> e.equalsIgnoreCase(entitlement.name()));
        }
        return entitled;
    }
}

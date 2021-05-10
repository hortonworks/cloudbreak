package com.sequenceiq.authorization.service;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class UmsAccountAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAccountAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private EntitlementService entitlementService;

    public void checkRightOfUser(String userCrn, AuthorizationResourceAction action) {
        String right = umsRightProvider.getRight(action);
        String unauthorizedMessage = String.format("You have no right to perform %s in account %s", right,
                Crn.fromString(userCrn).getAccountId());
        checkRightOfUser(userCrn, right, unauthorizedMessage);
    }

    private void checkRightOfUser(String userCrn, String right, String unauthorizedMessage) {
        if (entitlementService.isAuthorizationEntitlementRegistered(ThreadBasedUserCrnProvider.getAccountId())) {
            if (!umsClient.checkAccountRight(userCrn, right, getRequestId())) {
                LOGGER.error(unauthorizedMessage);
                throw new AccessDeniedException(unauthorizedMessage);
            }
        } else {
            if (!umsClient.checkAccountRightLegacy(userCrn, right, getRequestId())) {
                LOGGER.error(unauthorizedMessage);
                throw new AccessDeniedException(unauthorizedMessage);
            }
        }
    }

    // Checks that the calling actor is either performing an action against themselves or have the right
    public void checkCallerIsSelfOrHasRight(String actorCrnStr, String targetUserCrnStr, AuthorizationResourceAction action) {
        Crn actorCrn = Crn.safeFromString(actorCrnStr);
        Crn targetUserCrn = Crn.safeFromString(targetUserCrnStr);
        if (actorCrn.equals(targetUserCrn)) {
            return;
        }
        if (!actorCrn.getAccountId().equals(targetUserCrn.getAccountId())) {
            String unauthorizedMessage = "Unauthorized to run this operation in a different account";
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
        checkRightOfUser(actorCrnStr, action);
    }

    protected Optional<String> getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return Optional.of(requestId);
    }
}

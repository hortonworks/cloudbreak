package com.sequenceiq.authorization.service;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceActionType;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
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

    public void checkRightOfUser(String userCrn, AuthorizationResourceAction action) {
        String unauthorizedMessage = String.format("You have no right to perform %s in account %s.", umsRightProvider.getRight(action),
                Crn.fromString(userCrn).getAccountId());
        checkRightOfUser(userCrn, action, unauthorizedMessage);
    }

    public Boolean hasRightOfUser(String userCrn, String action) {
        Optional<AuthorizationResourceAction> actionEnum = umsRightProvider.getByName(action);
        if (!actionEnum.isPresent()) {
            throw new BadRequestException("Action cannot be found by request!");
        }
        if (!hasRightOfUser(userCrn, actionEnum.get())) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private void checkRightOfUser(String userCrn, AuthorizationResourceAction action, String unauthorizedMessage) {
        if (!hasRightOfUser(userCrn, action)) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    private boolean hasRightOfUser(String userCrn, AuthorizationResourceAction action) {
        validateAction(action);
        return umsClient.checkRight(userCrn, userCrn, umsRightProvider.getRight(action), getRequestId());
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
        validateAction(action);
        if (!umsClient.checkRight(GrpcUmsClient.INTERNAL_ACTOR_CRN, actorCrn.toString(), umsRightProvider.getRight(action), getRequestId())) {
            String unauthorizedMessage = String.format("You have no right to perform %s on user %s.", umsRightProvider.getRight(action), targetUserCrnStr);
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    private void validateAction(AuthorizationResourceAction action) {
        if (umsRightProvider.getActionType(action).equals(AuthorizationResourceActionType.RESOURCE_DEPENDENT)) {
            throw new UnsupportedOperationException("TODO");
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

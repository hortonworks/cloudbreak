package com.sequenceiq.authorization.service;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

@Service
public class UmsAccountAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAccountAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private UmsRightProvider umsRightProvider;

    public void checkRightOfUser(String userCrn, AuthorizationResourceAction action) {
        String right = umsRightProvider.getRight(action);
        String unauthorizedMessage = String.format("You have no right to perform %s in account %s", right, Crn.fromString(userCrn).getAccountId());
        checkRightOfUser(userCrn, right, unauthorizedMessage);
    }

    private void checkRightOfUser(String userCrn, String right, String unauthorizedMessage) {
        if (!umsClient.checkAccountRight(userCrn, right)) {
            LOGGER.error(unauthorizedMessage);
            throw new ForbiddenException(unauthorizedMessage);
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
            throw new ForbiddenException(unauthorizedMessage);
        }
        checkRightOfUser(actorCrnStr, action);
    }
}

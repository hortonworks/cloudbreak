package com.sequenceiq.authorization.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Service
public class AccountAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountAuthorizationService.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    public void authorize(CheckPermissionByAccount methodAnnotation, String userCrn) {
        AuthorizationResourceAction action = methodAnnotation.action();
        LOGGER.debug("Authorize {} action for user {} on account level.", action, userCrn);
        commonPermissionCheckingUtils.checkPermissionForUser(action, userCrn);
    }
}

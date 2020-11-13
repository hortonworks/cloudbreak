package com.sequenceiq.authorization.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Service
public class AccountAuthorizationService {

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    public void authorize(CheckPermissionByAccount methodAnnotation, String userCrn) {
        AuthorizationResourceAction action = methodAnnotation.action();
        commonPermissionCheckingUtils.checkPermissionForUser(action, userCrn);
    }
}

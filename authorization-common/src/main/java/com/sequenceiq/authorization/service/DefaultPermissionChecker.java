package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Component
public class DefaultPermissionChecker implements PermissionChecker<CheckPermissionByAccount> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resource, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime) {
        CheckPermissionByAccount methodAnnotation = (CheckPermissionByAccount) rawMethodAnnotation;
        AuthorizationResourceAction action = methodAnnotation.action();
        checkActionType(resource, action);
        commonPermissionCheckingUtils.checkPermissionForUser(resource, action, userCrn);
    }

    @Override
    public Class<CheckPermissionByAccount> supportedAnnotation() {
        return CheckPermissionByAccount.class;
    }

    @Override
    public AuthorizationResourceAction.ActionType actionType() {
        return AuthorizationResourceAction.ActionType.RESOURCE_INDEPENDENT;
    }
}

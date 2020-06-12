package com.sequenceiq.authorization.service.defaults;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.PermissionChecker;

@Component
public class DefaultPermissionChecker implements PermissionChecker<CheckPermissionByAccount> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        CheckPermissionByAccount methodAnnotation = (CheckPermissionByAccount) rawMethodAnnotation;
        AuthorizationResourceAction action = methodAnnotation.action();
        commonPermissionCheckingUtils.checkPermissionForUser(action, userCrn);
    }

    @Override
    public Class<CheckPermissionByAccount> supportedAnnotation() {
        return CheckPermissionByAccount.class;
    }
}

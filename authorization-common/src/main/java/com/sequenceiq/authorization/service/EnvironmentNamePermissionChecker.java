package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentName;
import com.sequenceiq.authorization.annotation.EnvironmentName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Component
public class EnvironmentNamePermissionChecker implements PermissionChecker<CheckPermissionByEnvironmentName> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentNamePermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        CheckPermissionByEnvironmentName methodAnnotation = (CheckPermissionByEnvironmentName) rawMethodAnnotation;
        AuthorizationResourceAction action = methodAnnotation.action();
        String environmentName = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, EnvironmentName.class, String.class);
        String resourceCrn = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action).getResourceCrnByEnvironmentName(environmentName);
        commonPermissionCheckingUtils.checkPermissionForUserOnResource(action, userCrn, resourceCrn);
    }

    @Override
    public Class<CheckPermissionByEnvironmentName> supportedAnnotation() {
        return CheckPermissionByEnvironmentName.class;
    }
}

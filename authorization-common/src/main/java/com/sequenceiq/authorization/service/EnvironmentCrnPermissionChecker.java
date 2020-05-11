package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentCrn;
import com.sequenceiq.authorization.annotation.EnvironmentCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@Component
public class EnvironmentCrnPermissionChecker implements PermissionChecker<CheckPermissionByEnvironmentCrn> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrnPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        CheckPermissionByEnvironmentCrn methodAnnotation = (CheckPermissionByEnvironmentCrn) rawMethodAnnotation;
        AuthorizationResourceAction action = methodAnnotation.action();
        String environmentCrn = commonPermissionCheckingUtils.getParameter(proceedingJoinPoint, methodSignature, EnvironmentCrn.class, String.class);
        String resourceCrn = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action).getResourceCrnByEnvironmentCrn(environmentCrn);
        commonPermissionCheckingUtils.checkPermissionForUserOnResource(action, userCrn, resourceCrn);
    }

    @Override
    public Class<CheckPermissionByEnvironmentCrn> supportedAnnotation() {
        return CheckPermissionByEnvironmentCrn.class;
    }
}

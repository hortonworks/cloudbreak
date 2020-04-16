package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

public interface PermissionChecker<T extends Annotation> {

    <T extends Annotation> void checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resource, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime);

    Class<T> supportedAnnotation();

    AuthorizationResourceAction.ActionType actionType();

    default void checkActionType(AuthorizationResourceType type, AuthorizationResourceAction action) {
        if (!action.getActionType().equals(actionType())) {
            throw new IllegalStateException(String.format("Action %s on %s should be %s",
                    action.getAction(), type.getResource(), actionType()));
        }
    }
}

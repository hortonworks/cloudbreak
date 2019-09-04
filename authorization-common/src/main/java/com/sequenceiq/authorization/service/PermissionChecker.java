package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;

public interface PermissionChecker<T extends Annotation> {

    <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, AuthorizationResourceType resource, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime);

    Class<T> supportedAnnotation();
}

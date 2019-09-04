package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.sequenceiq.authorization.resource.ResourceType;

public interface PermissionChecker<T extends Annotation> {

    <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, ResourceType resource, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature);

    Class<T> supportedAnnotation();
}

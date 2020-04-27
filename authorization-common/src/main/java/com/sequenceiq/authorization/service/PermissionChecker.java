package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public interface PermissionChecker<T extends Annotation> {

    <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime);

    Class<T> supportedAnnotation();
}

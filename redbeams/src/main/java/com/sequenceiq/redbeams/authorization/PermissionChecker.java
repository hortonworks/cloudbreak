package com.sequenceiq.redbeams.authorization;

import java.lang.annotation.Annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public interface PermissionChecker<T extends Annotation> {

    // CHECKSTYLE:OFF
    <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature) throws Throwable;
    // CHECKSTYLE:ON

    Class<T> supportedAnnotation();
}

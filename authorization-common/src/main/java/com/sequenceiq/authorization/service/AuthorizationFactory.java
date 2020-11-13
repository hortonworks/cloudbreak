package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.sequenceiq.authorization.service.model.AuthorizationRule;

public interface AuthorizationFactory<T extends Annotation> {

    Optional<AuthorizationRule> getAuthorization(Annotation rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature);

    Class<T> supportedAnnotation();
}

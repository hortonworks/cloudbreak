package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.sequenceiq.authorization.service.model.AuthorizationRule;

public abstract class TypedAuthorizationFactory<T extends Annotation> implements AuthorizationFactory<T> {
    @Override
    public Optional<AuthorizationRule> getAuthorization(Annotation rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature) {
        T annotation = (T) rawMethodAnnotation;
        return doGetAuthorization(annotation, userCrn, proceedingJoinPoint, methodSignature);
    }

    abstract Optional<AuthorizationRule> doGetAuthorization(T methodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature);
}

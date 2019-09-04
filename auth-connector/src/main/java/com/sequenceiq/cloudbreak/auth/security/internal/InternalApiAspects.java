package com.sequenceiq.cloudbreak.auth.security.internal;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Order(0)
public class InternalApiAspects {

    @Inject
    private InternalCrnModifier internalCrnModifier;

    @Pointcut("within(@com.sequenceiq.cloudbreak.auth.security.internal.InternalReady *)")
    public void allEndpoints() {
    }

    @Around("allEndpoints()")
    public Object changeInternalCrn(ProceedingJoinPoint proceedingJoinPoint) {
        return internalCrnModifier.changeInternalCrn(proceedingJoinPoint);
    }

}

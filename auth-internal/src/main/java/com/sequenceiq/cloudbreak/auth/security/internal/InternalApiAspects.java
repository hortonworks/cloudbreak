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

    @Pointcut("execution(* com.sequenceiq..*.*(..))")
    public void onlyCloudbreakControllers() {
    }

    @Pointcut("within(@org.springframework.stereotype.Controller *)")
    public void controllerClass() {
    }

    @Around("controllerClass() && onlyCloudbreakControllers()")
    public Object changeInternalCrn(ProceedingJoinPoint proceedingJoinPoint) {
        return internalCrnModifier.changeInternalCrn(proceedingJoinPoint);
    }

}

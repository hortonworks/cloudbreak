package com.sequenceiq.redbeams.aspect;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.sequenceiq.redbeams.authorization.PermissionCheckerService;

@Component
@Aspect
public class CheckPermissionsAspects {

    @Inject
    private PermissionCheckerService permissionCheckerService;

    @Pointcut("execution(public * com.sequenceiq.redbeams.repository..*.*(..)) ")
    public void allRepositories() {
    }

    @Around("allRepositories()")
    // CHECKSTYLE:OFF
    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
    // CHECKSTYLE:ON
        return permissionCheckerService.hasPermission(proceedingJoinPoint);
    }
}

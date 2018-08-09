package com.sequenceiq.cloudbreak.aspect;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.authorization.PermissionCheckerService;

@Component
@Aspect
public class CheckPermissionsAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckPermissionsAspects.class);

    @Inject
    private PermissionCheckerService permissionCheckerService;

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository.*.*(..)) ")
    public void allRepositories2() {
    }

    @Around("allRepositories2()")
    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) {
        return permissionCheckerService.hasPermission(proceedingJoinPoint);
    }
}

package com.sequenceiq.authorization.aspect;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.PermissionCheckService;

@Component
@Aspect
public class CheckPermissionsAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckPermissionsAspects.class);

    @Inject
    private PermissionCheckService permissionCheckerService;

    @Pointcut("execution(* com.sequenceiq..*.*(..))")
    public void onlyCloudbreakControllers() {
    }

    @Pointcut("within(@org.springframework.stereotype.Controller *)")
    public void controllerClass() {
    }

    @Around("controllerClass() && onlyCloudbreakControllers()")
    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) {
        return permissionCheckerService.hasPermission(proceedingJoinPoint);
    }
}

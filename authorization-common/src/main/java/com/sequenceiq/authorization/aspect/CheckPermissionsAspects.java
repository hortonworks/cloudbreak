package com.sequenceiq.authorization.aspect;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.AbstractPermissionCheckerService;

@Component
@Aspect
public class CheckPermissionsAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckPermissionsAspects.class);

    @Inject
    private AbstractPermissionCheckerService permissionCheckerService;

    @Pointcut("within(@org.springframework.stereotype.Controller *) "
            + "&& within(@com.sequenceiq.authorization.resource.AuthorizationResource *)")
    public void allEndpoints() {
    }

    @Around("allEndpoints()")
    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) {
        return permissionCheckerService.hasPermission(proceedingJoinPoint);
    }
}

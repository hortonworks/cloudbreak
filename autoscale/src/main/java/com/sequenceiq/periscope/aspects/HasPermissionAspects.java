package com.sequenceiq.periscope.aspects;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.workspace.HasPermissionService;

@Component
@Aspect
public class HasPermissionAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(HasPermissionAspects.class);

    @Inject
    private HasPermissionService hasPermissionService;

    @Pointcut("execution(public * com.sequenceiq.periscope.repository.*.*(..)) ")
    public void allRepositories() {
    }

    @Around("allRepositories()")
    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return hasPermissionService.hasPermission(proceedingJoinPoint);
    }
}

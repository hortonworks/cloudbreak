package com.sequenceiq.periscope.log;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.service.AuthenticatedUserService;

@Component
@Aspect
public class ControllerLogContextAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerLogContextAspects.class);

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Pointcut("execution(public * com.sequenceiq.periscope.controller.*Controller.*(..))")
    public void interceptControllerMethodCalls() {
    }

    @Before("com.sequenceiq.periscope.log.ControllerLogContextAspects.interceptControllerMethodCalls()")
    public void buildLogContextForControllerCalls(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        CodeSignature sig = (CodeSignature) joinPoint.getSignature();
        String[] paramNames = sig.getParameterNames();
        Long clusterId = getClusterId(paramNames, args);
        PeriscopeUser user = authenticatedUserService.getPeriscopeUser();
        MDCBuilder.buildMdcContext(user, clusterId);
        LOGGER.debug("A controller method has been intercepted: {} with params {}, {}, MDC logger context is built.", joinPoint.toShortString(),
                sig.getParameterNames(), args);
    }

    private Long getClusterId(String[] paramNames, Object[] args) {
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i].toLowerCase();
            Object paramValue = args[i];
            if ("clusterid".equals(paramName) && paramValue instanceof Long) {
                return (Long) paramValue;
            }
        }
        return null;
    }
}

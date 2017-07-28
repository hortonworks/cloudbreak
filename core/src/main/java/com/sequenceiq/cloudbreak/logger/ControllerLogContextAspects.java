package com.sequenceiq.cloudbreak.logger;

import java.util.Map;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.controller.AuthenticatedUserService;

@Component
@Aspect
public class ControllerLogContextAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerLogContextAspects.class);

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.controller.*Controller.*(..))")
    public void interceptControllerMethodCalls() {
    }

    @Before("com.sequenceiq.cloudbreak.logger.ControllerLogContextAspects.interceptControllerMethodCalls()")
    public void buildLogContextForControllerCalls(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = sig.getParameterNames();
        Map<String, String> mdcParams = getMDCParams(joinPoint.getTarget(), paramNames, args);
        MDCBuilder.buildMdcContextFromMap(mdcParams);
        LOGGER.debug("A controller method has been intercepted: {} with params {}, {}, MDC logger context is built.", joinPoint.toShortString(),
                sig.getParameterNames(), args);
    }

    private Map<String, String> getMDCParams(Object target, String[] paramNames, Object[] args) {
        Map<String, String> result = Maps.newHashMap();
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i].toLowerCase();
            Object paramValue = args[i];
            if (paramName.contains("name")) {
                result.put(LoggerContextKey.RESOURCE_NAME.toString(), paramValue.toString());
            } else if (paramName.contains("id")) {
                result.put(LoggerContextKey.RESOURCE_ID.toString(), paramValue.toString());
            } else if (paramName.contains("request")) {
                result.put(LoggerContextKey.RESOURCE_NAME.toString(), MDCBuilder.getFieldValue(args[i], "name"));
            }
        }
        String controllerClassName = target.getClass().getSimpleName();
        String resourceType = controllerClassName.substring(0, controllerClassName.indexOf("Controller"));
        result.put(LoggerContextKey.RESOURCE_TYPE.toString(), resourceType);
        result.put(LoggerContextKey.OWNER_ID.toString(), authenticatedUserService.getCbUser().getUserId());
        return result;
    }
}

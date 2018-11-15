package com.sequenceiq.cloudbreak.logger;

import java.util.Map;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Aspect
public class ControllerLogContextAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerLogContextAspects.class);

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.controller.*Controller.*(..))")
    public void interceptControllerMethodCalls() {
    }

    @Before("com.sequenceiq.cloudbreak.logger.ControllerLogContextAspects.interceptControllerMethodCalls()")
    public void buildLogContextForControllerCalls(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            CodeSignature sig = (CodeSignature) joinPoint.getSignature();
            String[] paramNames = sig.getParameterNames();
            String trackingIdLCKey = LoggerContextKey.TRACKING_ID.toString();
            Map<String, String> mdcParams = getMDCParams(joinPoint.getTarget(), paramNames, args);
            mdcParams.put(trackingIdLCKey, MDCBuilder.getMdcContextMap().get(trackingIdLCKey));
            MDCBuilder.buildMdcContextFromMap(mdcParams);
            LOGGER.debug("A controller method has been intercepted: {} with params {}, {}, MDC logger context is built.", joinPoint.toShortString(),
                    sig.getParameterNames(), args);
        } catch (Exception any) {
            LOGGER.warn("MDCContext build failed: ", any);
        }
    }

    private Map<String, String> getMDCParams(Object target, String[] paramNames, Object[] args) {
        Map<String, String> result = Maps.newHashMap();
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i].toLowerCase();
            Object paramValue = args[i];
            String paramString = paramValue != null ? paramValue.toString() : "";
            if (paramName.contains("name")) {
                result.put(LoggerContextKey.RESOURCE_NAME.toString(), paramString);
            } else if (paramName.contains("id")) {
                result.put(LoggerContextKey.RESOURCE_ID.toString(), paramString);
            } else if (paramName.contains("request")) {
                result.put(LoggerContextKey.RESOURCE_NAME.toString(), MDCBuilder.getFieldValue(paramValue, "name"));
            }
        }
        String controllerClassName = target.getClass().getSimpleName();
        String resourceType = controllerClassName.substring(0, controllerClassName.indexOf("Controller"));
        result.put(LoggerContextKey.RESOURCE_TYPE.toString(), resourceType);
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        String userName = cloudbreakUser != null ? userService.getOrCreate(cloudbreakUser).getUserName() : "undefined";
        result.put(LoggerContextKey.USER.toString(), userName);
        return result;
    }
}

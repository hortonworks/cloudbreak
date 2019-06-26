package com.sequenceiq.freeipa.logger;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.LogContextService;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Component
@Aspect
public class ControllerLogContextAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerLogContextAspects.class);

    @Inject
    private LogContextService logContextService;

    @Pointcut("execution(public * com.sequenceiq.freeipa.**.*Controller.*(..))")
    public void interceptControllerMethodCalls() {
    }

    @Before("com.sequenceiq.freeipa.logger.ControllerLogContextAspects.interceptControllerMethodCalls()")
    public void buildLogContextForControllerCalls(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            CodeSignature sig = (CodeSignature) joinPoint.getSignature();
            String[] paramNames = sig.getParameterNames();
            logContextService.buildMDCParams(joinPoint.getTarget(), paramNames, args);
            MDCBuilder.addRequestIdToMdcContext(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
            LOGGER.debug("A controller method has been intercepted: {} with params {}, {}, MDC logger context is built.", joinPoint.toShortString(),
                    sig.getParameterNames(), args);
        } catch (Exception any) {
            LOGGER.warn("MDCContext build failed: ", any);
        }
    }
}

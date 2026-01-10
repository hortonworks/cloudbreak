package com.sequenceiq.cloudbreak.logger;

import java.util.Arrays;

import jakarta.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;

@Component
@Aspect
public class ControllerLogContextAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerLogContextAspects.class);

    @Inject
    private LogContextService logContextService;

    @Pointcut("execution(public * com.sequenceiq..*.*Controller.*(..))")
    public void interceptControllerMethodCalls() {
    }

    @Before("com.sequenceiq.cloudbreak.logger.ControllerLogContextAspects.interceptControllerMethodCalls()")
    public void buildLogContextForControllerCalls(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            CodeSignature sig = (CodeSignature) joinPoint.getSignature();
            String[] paramNames = sig.getParameterNames();
            logContextService.buildMDCParams(joinPoint.getTarget(), paramNames, args);
            MDCBuilder.getOrGenerateRequestId();
            LOGGER.trace("A controller method has been intercepted: {} with params {}, {}, MDC logger context is built.", joinPoint.toShortString(),
                    sig.getParameterNames(), AnonymizerUtil.anonymize(Arrays.toString(args)));
        } catch (Exception any) {
            LOGGER.warn("MDCContext build failed: ", any);
        }
    }
}

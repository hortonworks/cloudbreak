package com.sequenceiq.redbeams.aspect;

import java.util.Arrays;

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

@Component
@Aspect
public class ControllerLogContextAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerLogContextAspects.class);

    @Inject
    private LogContextService logContextService;

    @Pointcut("execution(public * com.sequenceiq.redbeams.controller..*(..))")
    public void interceptControllerMethodCalls() {
    }

    @Before("com.sequenceiq.redbeams.aspect.ControllerLogContextAspects.interceptControllerMethodCalls()")
    public void buildLogContextForControllerCalls(JoinPoint joinPoint) {

        LOGGER.debug("Intercepted controller method {}", joinPoint.toShortString());

        try {
            Object[] args = joinPoint.getArgs();
            CodeSignature sig = (CodeSignature) joinPoint.getSignature();
            String[] paramNames = sig.getParameterNames();

            // FIXME Once CB-1923 is done, this param name patching can be removed
            // This does not help getting the environment CRN from a request object anyway
            for (int i = 0; i < paramNames.length; i++) {
                if (paramNames[i].equalsIgnoreCase("environmentid")) {
                    paramNames[i] = "environmentCrn";
                }
            }
            logContextService.buildMDCParams(joinPoint.getTarget(), paramNames, args);

            LOGGER.debug("Added selected controller method parameters to MDC; param names {}", Arrays.toString(paramNames));
        } catch (Exception e) {
            LOGGER.warn("Failed to add controller method parameters to MDC, continuing with call", e);
        }
    }
}

package com.sequenceiq.cloudbreak.cm.client.retry;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CmApiRetryAspect {

    private final RetryTemplate cmApiRetryTemplate;

    public CmApiRetryAspect(RetryTemplate cmApiRetryTemplate) {
        this.cmApiRetryTemplate = cmApiRetryTemplate;
    }

    @Pointcut("within(com.cloudera.api.swagger.*) && !execution(public * *(com.cloudera.api.swagger.client.ApiClient))")
    public void allCmApiCalls() {
    }

    @Pointcut("!execution(public * com.cloudera.api.swagger.ClouderaManagerResourceApi.getVersion())")
    public void noGetVersion() {
    }

    @Around("allCmApiCalls() && noGetVersion()")
    // CHECKSTYLE:OFF
    public Object retryableApiCall(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // CHECKSTYLE:ON
        return cmApiRetryTemplate.execute(context -> proceedingJoinPoint.proceed());
    }
}

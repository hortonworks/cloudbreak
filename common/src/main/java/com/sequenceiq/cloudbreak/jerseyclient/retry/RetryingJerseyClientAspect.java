package com.sequenceiq.cloudbreak.jerseyclient.retry;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RetryingJerseyClientAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryingJerseyClientAspect.class);

    @Inject
    private RetryTemplate jerseyClientRetryTemplate;

    @Pointcut("within(@com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient *)")
    public void beanAnnotatedWithRetryingRestClient() {
    }

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @Pointcut("publicMethod() && beanAnnotatedWithRetryingRestClient()")
    public void publicMethodInsideAClassMarkedWithAtRetryingRestClient() {
    }

    @Around("publicMethodInsideAClassMarkedWithAtRetryingRestClient()")
    // CHECKSTYLE:OFF
    public Object retryableRestApiCall(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // CHECKSTYLE:ON
        try {
            return jerseyClientRetryTemplate.execute(context -> proceedingJoinPoint.proceed());
        } catch (WebApplicationException e) {
            LOGGER.error("Failed to execute REST API call with retries.", e);
            throw e;
        }
    }

}

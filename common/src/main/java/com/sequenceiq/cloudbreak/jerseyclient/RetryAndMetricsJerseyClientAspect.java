package com.sequenceiq.cloudbreak.jerseyclient;

import java.lang.reflect.Field;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricTag;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;

@Aspect
@Component
public class RetryAndMetricsJerseyClientAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryAndMetricsJerseyClientAspect.class);

    @Inject
    private RetryTemplate jerseyClientRetryTemplate;

    @Inject
    private MetricService metricService;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Pointcut("within(@com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics *)")
    public void beanAnnotatedWithRetryAndMetrics() {
    }

    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    @Pointcut("publicMethod() && beanAnnotatedWithRetryAndMetrics()")
    public void publicMethodInsideAClassMarkedWithRetryAndMetrics() {
    }

    @Around("publicMethodInsideAClassMarkedWithRetryAndMetrics()")
    // CHECKSTYLE:OFF
    public Object restApiCallWithRetryAndMetrics(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // CHECKSTYLE:ON
        String invokedApi = proceedingJoinPoint.getSignature().getDeclaringType().getSimpleName();
        String invokedMethod = proceedingJoinPoint.getSignature().getName();
        try {
            Object result = jerseyClientRetryTemplate.execute(context -> proceedingJoinPoint.proceed());
            metricService.incrementMetricCounter(MetricType.REST_OPERATION,
                    MetricTag.TARGET_API.name(), invokedApi,
                    MetricTag.TARGET_METHOD.name(), invokedMethod);
            return result;
        } catch (WebApplicationException webApplicationException) {
            throw handleWebApplicationException(invokedApi, invokedMethod, webApplicationException);
        } catch (Exception e) {
            incrementMetricCounter(invokedApi, invokedMethod, e);
            throw e;
        }
    }

    private WebApplicationException handleWebApplicationException(String invokedApi, String invokedMethod, WebApplicationException webApplicationException) {
        incrementMetricCounter(invokedApi, invokedMethod, webApplicationException);
        try {
            String errorMessage = exceptionMessageExtractor.getErrorMessage(webApplicationException);
            if (StringUtils.isNotBlank(errorMessage)) {
                Field detailMessage = Throwable.class.getDeclaredField("detailMessage");
                detailMessage.trySetAccessible();
                detailMessage.set(webApplicationException, errorMessage);
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't extract message from exception", e);
        }
        return webApplicationException;
    }

    private void incrementMetricCounter(String invokedApi, String invokedMethod, Exception e) {
        LOGGER.error("Failed to execute REST API call with retries.", e);
        metricService.incrementMetricCounter(MetricType.REST_OPERATION_FAILED,
                MetricTag.TARGET_API.name(), invokedApi,
                MetricTag.TARGET_METHOD.name(), invokedMethod,
                MetricTag.EXCEPTION_TYPE.name(), e.getClass().getName());
    }
}

package com.sequenceiq.cloudbreak.jerseyclient;

import static com.sequenceiq.cloudbreak.quartz.configuration.SchedulerFactoryConfig.QUARTZ_EXECUTOR_THREAD_NAME_PREFIX;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

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
    private RetryTemplate quartzJerseyClientRetryTemplate;

    @Inject
    private MetricService metricService;

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
            Object result;
            if (Thread.currentThread().getName().contains(QUARTZ_EXECUTOR_THREAD_NAME_PREFIX)) {
                result = quartzJerseyClientRetryTemplate.execute(context -> proceedingJoinPoint.proceed());
            } else {
                result = jerseyClientRetryTemplate.execute(context -> proceedingJoinPoint.proceed());
            }
            metricService.incrementMetricCounter(MetricType.REST_OPERATION,
                    MetricTag.TARGET_API.name(), invokedApi,
                    MetricTag.TARGET_METHOD.name(), invokedMethod);
            return result;
        } catch (NotFoundException nFE) {
            LOGGER.debug("API call failed with 404 not found error code.", nFE);
            incrementMetricCounter(invokedApi, invokedMethod, nFE);
            throw nFE;
        } catch (Exception e) {
            LOGGER.error("Failed to execute REST API call with retries.", e);
            incrementMetricCounter(invokedApi, invokedMethod, e);
            throw e;
        }
    }

    private void incrementMetricCounter(String invokedApi, String invokedMethod, Exception e) throws Exception {
        metricService.incrementMetricCounter(MetricType.REST_OPERATION_FAILED,
                MetricTag.TARGET_API.name(), invokedApi,
                MetricTag.TARGET_METHOD.name(), invokedMethod,
                MetricTag.EXCEPTION_TYPE.name(), e.getClass().getName());
    }

}

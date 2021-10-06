package com.sequenceiq.cloudbreak.cm.client.transaction;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sequenceiq.cloudbreak.common.tx.CircuitBreakerType;

@Aspect
@Component
public class CmApiDetectTransactionAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmApiDetectTransactionAspect.class);

    @Value("${environment.hibernate.circuitbreaker:LOG}")
    private CircuitBreakerType circuitBreakerType;

    @Pointcut("within(com.cloudera.api.swagger.*) && !execution(public * *(com.cloudera.api.swagger.client.ApiClient))")
    public void allCmApiCalls() {
    }

    @Around("allCmApiCalls()")
    // CHECKSTYLE:OFF
    public Object retryableApiCall(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // CHECKSTYLE:ON
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            CmApiCallInTransactionException exception = new CmApiCallInTransactionException("CM api call detected during transaction.");
            LOGGER.error("", exception);
            if (circuitBreakerType == CircuitBreakerType.BREAK) {
                throw exception;
            }
        }
        return proceedingJoinPoint.proceed();
    }
}

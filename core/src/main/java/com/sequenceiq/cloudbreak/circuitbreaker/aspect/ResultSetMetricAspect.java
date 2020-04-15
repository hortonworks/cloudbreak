package com.sequenceiq.cloudbreak.circuitbreaker.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#aop
// https://www.baeldung.com/spring-aop-pointcut-tutorial

@Component
@Aspect
public class ResultSetMetricAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultSetMetricAspect.class);

    public ResultSetMetricAspect() {
        LOGGER.debug("Init ResultSetMetricAspect");
    }

    //@Pointcut("execution(public java.sql.ResultSet com.sequenceiq.cloudbreak.circuitbreaker.jdbc.CircuitBreakerPreparedStatement.executeQuery())")
    @Pointcut("execution(public java.sql.ResultSet io.opentracing.contrib.jdbc.TracingPreparedStatement.executeQuery())")
    public void interceptExecuteQuery() {
    }

    @Around("interceptExecuteQuery()")
    public Object executeQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.debug("around executeQuery");
        return joinPoint.proceed();
    }
}

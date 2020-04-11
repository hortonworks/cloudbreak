package com.sequenceiq.cloudbreak.aspect;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ResultSetMetricAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResultSetMetricAspect.class);
    // https://www.baeldung.com/spring-aop-pointcut-tutorial

    //@Pointcut("execution(public java.sql.ResultSet org.hibernate.engine.jdbc.internal.ResultSetReturnImpl.extract(java.sql.PreparedStatement))")
    @Pointcut("execution(public java.sql.ResultSet io.opentracing.contrib.jdbc.TracingPreparedStatement.executeQuery())")
    //@Pointcut("target(io.opentracing.contrib.jdbc.TracingPreparedStatement)")
    public void interceptExecuteQuery() {
    }

    @Around("interceptExecuteQuery()")
    public Object executeQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.debug("around executeQuery");
        return joinPoint.proceed();
    }
}
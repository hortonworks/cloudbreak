package com.sequenceiq.cloudbreak.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class MeasureAspects {

    @Pointcut("execution(@Measure * *(..))")
    public void isAnnotated() {
    }

    @Around("isAnnotated()")
    public Object proceed(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Measure measure = methodSignature.getMethod().getAnnotation(Measure.class);
        if (measure == null) {
            return proceedingJoinPoint.proceed();
        }
        long start = System.currentTimeMillis();
        try {
            return proceedingJoinPoint.proceed();
        } finally {
            LoggerFactory.getLogger(measure.value()).debug("Measuring method execution: {}.{} took {} ms",
                    methodSignature.getDeclaringType().getSimpleName(), methodSignature.getName(), System.currentTimeMillis() - start);
        }
    }
}

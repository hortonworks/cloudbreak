package com.sequenceiq.cloudbreak.auth.security.internal;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

@Service
public class ReflectionUtil {

    public Optional<Object> getParameter(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, Class annotation) {
        Optional<Parameter> optionalParameter = Arrays.stream(methodSignature.getMethod().getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(annotation))
                .findFirst();
        if (optionalParameter.isPresent()) {
            int parameterIndex = Lists.newArrayList(methodSignature.getMethod().getParameters()).indexOf(optionalParameter.get());
            return Optional.of(proceedingJoinPoint.getArgs()[parameterIndex]);
        }
        return Optional.empty();
    }

    public Object proceed(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        try {
            return proceedingJoinPoint.proceed();
        } catch (Error | RuntimeException unchecked) {
            throw unchecked;
        } catch (Throwable t) {
            throw new AccessDeniedException(t.getMessage(), t);
        }
    }
}

package com.sequenceiq.authorization.service;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.annotation.EnvironmentCrn;
import com.sequenceiq.authorization.annotation.EnvironmentName;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.api.EnvironmentCrnAwareApiModel;
import com.sequenceiq.authorization.api.EnvironmentNameAwareApiModel;

public class EnvironmentPermissionCheckerUtil {

    private EnvironmentPermissionCheckerUtil() {
    }

    public static String getResourceCrn(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        Object result = getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class);
        return (String) result;
    }

    public static String getResourceName(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        Object result = getParameter(proceedingJoinPoint, methodSignature, ResourceName.class);
        return (String) result;
    }

    public static String getEnvironmentCrn(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        Object result = getParameter(proceedingJoinPoint, methodSignature, EnvironmentCrn.class);
        return EnvironmentCrnAwareApiModel.class.isAssignableFrom(result.getClass()) ?
                ((EnvironmentCrnAwareApiModel) result).getEnvironmentCrn() : (String) result;

    }

    public static String getEnvironmentName(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        Object result = getParameter(proceedingJoinPoint, methodSignature, EnvironmentName.class);
        return EnvironmentNameAwareApiModel.class.isAssignableFrom(result.getClass()) ?
                ((EnvironmentNameAwareApiModel) result).getEnvironmentName() : (String) result;
    }

    private static Object getParameter(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, Class annotation) {
        Optional<Parameter> optionalParameter = Arrays.stream(methodSignature.getMethod().getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(annotation))
                .findFirst();
        if (!optionalParameter.isPresent()) {
            throw new IllegalStateException(String.format("Your controller method should have a parameter with the annotation %s", annotation.getSimpleName()));
        }
        return proceedingJoinPoint.getArgs()[Lists.newArrayList(methodSignature.getMethod().getParameters()).indexOf(optionalParameter.get())];
    }

}

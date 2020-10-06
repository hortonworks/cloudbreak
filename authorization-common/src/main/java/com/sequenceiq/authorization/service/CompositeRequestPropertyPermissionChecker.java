package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByCompositeRequestProperty;

@Component
public class CompositeRequestPropertyPermissionChecker extends ResourcePermissionChecker<CheckPermissionByCompositeRequestProperty> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeRequestPropertyPermissionChecker.class);

    @Inject
    private RequestPropertyPermissionChecker requestPropertyPermissionChecker;

    @Override
    public <T extends Annotation> void checkPermissions(T rawMethodAnnotation, String userCrn, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature, long startTime) {
        // check fields of requestObject
        CheckPermissionByCompositeRequestProperty methodAnnotation = (CheckPermissionByCompositeRequestProperty) rawMethodAnnotation;
        Arrays.stream(methodAnnotation.value()).forEach(checkPermissionByRequestField -> requestPropertyPermissionChecker.
                checkPermissions(checkPermissionByRequestField, userCrn, proceedingJoinPoint, methodSignature, startTime));
    }

    @Override
    public Class<CheckPermissionByCompositeRequestProperty> supportedAnnotation() {
        return CheckPermissionByCompositeRequestProperty.class;
    }
}

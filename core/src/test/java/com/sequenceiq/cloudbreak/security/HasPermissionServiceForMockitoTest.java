package com.sequenceiq.cloudbreak.security;

import java.lang.reflect.ParameterizedType;

import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.HasPermissionService;

@Component
public class HasPermissionServiceForMockitoTest extends HasPermissionService {

    @Override
    protected ParameterizedType getParameterizedType(JoinPoint proceedingJoinPoint) {
        return (ParameterizedType) proceedingJoinPoint.getSignature().getDeclaringType().getInterfaces()[0].getGenericInterfaces()[0];
    }

}

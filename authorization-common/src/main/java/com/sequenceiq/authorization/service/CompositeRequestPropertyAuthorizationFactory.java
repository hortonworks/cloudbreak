package com.sequenceiq.authorization.service;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.CheckPermissionByCompositeRequestProperty;
import com.sequenceiq.authorization.service.model.AllMatch;
import com.sequenceiq.authorization.service.model.AuthorizationRule;

@Component
public class CompositeRequestPropertyAuthorizationFactory extends TypedAuthorizationFactory<CheckPermissionByCompositeRequestProperty> {

    @Inject
    private RequestPropertyAuthorizationFactory requestPropertyAuthorizationFactory;

    @Override
    public Optional<AuthorizationRule> doGetAuthorization(CheckPermissionByCompositeRequestProperty methodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        List<Optional<AuthorizationRule>> authorizations = Arrays.stream(methodAnnotation.value())
                .map(field -> requestPropertyAuthorizationFactory.getAuthorization(field, userCrn, proceedingJoinPoint, methodSignature))
                .collect(toList());
        return AllMatch.from(authorizations);
    }

    @Override
    public Class<CheckPermissionByCompositeRequestProperty> supportedAnnotation() {
        return CheckPermissionByCompositeRequestProperty.class;
    }
}

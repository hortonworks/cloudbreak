package com.sequenceiq.authorization.service;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.authorization.annotation.CheckPermissionByCompositeRequestProperty;
import com.sequenceiq.authorization.service.model.AllMatch;
import com.sequenceiq.authorization.service.model.AuthorizationRule;

@Component
public class CompositeRequestPropertyAuthorizationFactory extends TypedAuthorizationFactory<CheckPermissionByCompositeRequestProperty> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeRequestPropertyAuthorizationFactory.class);

    @Inject
    private RequestPropertyAuthorizationFactory requestPropertyAuthorizationFactory;

    @Override
    public Optional<AuthorizationRule> doGetAuthorization(CheckPermissionByCompositeRequestProperty methodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        LOGGER.debug("Getting authorization rule to authorize user [{}] over properties [{}] of request object.", userCrn,
                Joiner.on(",").join(Arrays.stream(methodAnnotation.value()).map(property -> property.path()).collect(Collectors.toUnmodifiableList())));
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

package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.utils.CrnAccountValidator;

@Component
public class ResourceCrnListAuthorizationFactory extends TypedAuthorizationFactory<CheckPermissionByResourceCrnList> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCrnListAuthorizationFactory.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private DefaultResourceAuthorizationProvider defaultResourceAuthorizationProvider;

    @Inject
    private EnvironmentBasedAuthorizationProvider environmentBasedAuthorizationProvider;

    @Inject
    private CrnAccountValidator crnAccountValidator;

    @Override
    public Optional<AuthorizationRule> doGetAuthorization(CheckPermissionByResourceCrnList methodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        AuthorizationResourceAction action = methodAnnotation.action();
        Collection<String> resourceCrns = commonPermissionCheckingUtils
                .getParameter(proceedingJoinPoint, methodSignature, ResourceCrnList.class, Collection.class);
        crnAccountValidator.validateSameAccount(userCrn, resourceCrns);
        LOGGER.debug("Getting authorization rule to authorize user [{}] for action [{}] over resources [{}]", userCrn, action,
                Joiner.on(",").join(resourceCrns));
        return calcAuthorization(resourceCrns, action);
    }

    public Optional<AuthorizationRule> calcAuthorization(Collection<String> resourceCrns, AuthorizationResourceAction action) {
        if (CollectionUtils.isEmpty(resourceCrns)) {
            return Optional.empty();
        }
        return defaultResourceAuthorizationProvider.authorizeDefaultOrElseCompute(resourceCrns, action,
                filteredResourceCrns -> environmentBasedAuthorizationProvider.getAuthorizations(filteredResourceCrns, action));
    }

    @Override
    public Class<CheckPermissionByResourceCrnList> supportedAnnotation() {
        return CheckPermissionByResourceCrnList.class;
    }
}

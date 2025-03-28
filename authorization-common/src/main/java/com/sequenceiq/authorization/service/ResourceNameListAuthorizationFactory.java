package com.sequenceiq.authorization.service;

import static java.util.function.Predicate.not;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.model.AuthorizationRule;

@Component
public class ResourceNameListAuthorizationFactory extends TypedAuthorizationFactory<CheckPermissionByResourceNameList> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNameListAuthorizationFactory.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private ResourceCrnListAuthorizationFactory resourceCrnListAuthorizationFactory;

    @Inject
    private Map<AuthorizationResourceType, AuthorizationResourceCrnListProvider> resourceCrnListProviderMap;

    @Override
    public Optional<AuthorizationRule> doGetAuthorization(CheckPermissionByResourceNameList methodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        AuthorizationResourceAction action = methodAnnotation.action();
        Collection<String> resourceNames = commonPermissionCheckingUtils
                .getParameter(proceedingJoinPoint, methodSignature, ResourceNameList.class, Collection.class);
        LOGGER.debug("Getting authorization rule to authorize user [{}] for action [{}] over resources [{}]", userCrn, action,
                Joiner.on(",").join(resourceNames));
        return calcAuthorization(resourceNames, action);
    }

    public Optional<AuthorizationRule> calcAuthorization(Collection<String> resourceNames, AuthorizationResourceAction action) {
        if (CollectionUtils.isEmpty(resourceNames)) {
            return Optional.empty();
        }
        AuthorizationResourceCrnListProvider authorizationResourceCrnListProvider = resourceCrnListProviderMap.get(action.getAuthorizationResourceType());
        if (authorizationResourceCrnListProvider == null) {
            LOGGER.error("There is no resource based crn provider implemented for action {} against resources {}, " +
                    "thus authorization is failing automatically.", action, Joiner.on(",").join(resourceNames));
            throw new ForbiddenException(String.format("Action %s is not supported over resources %s, thus access is denied",
                    action.getRight(), Joiner.on(",").join(resourceNames)));
        }
        List<String> resourceCrns = authorizationResourceCrnListProvider
                .getResourceCrnListByResourceNameList(getNotNullValues(resourceNames));
        return resourceCrnListAuthorizationFactory.calcAuthorization(getNotNullValues(resourceCrns), action);
    }

    private List<String> getNotNullValues(Collection<String> values) {
        return values.stream()
                .filter(not(StringUtils::isBlank))
                .toList();
    }

    @Override
    public Class<CheckPermissionByResourceNameList> supportedAnnotation() {
        return CheckPermissionByResourceNameList.class;
    }
}

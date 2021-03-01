package com.sequenceiq.authorization.service;

import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.defaults.CrnsByCategory;
import com.sequenceiq.authorization.service.defaults.DefaultResourceChecker;
import com.sequenceiq.authorization.utils.AuthorizationMessageUtilsService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class CommonPermissionCheckingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonPermissionCheckingUtils.class);

    @Inject
    private UmsAccountAuthorizationService umsAccountAuthorizationService;

    @Inject
    private UmsResourceAuthorizationService umsResourceAuthorizationService;

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private Map<AuthorizationResourceType, DefaultResourceChecker> defaultResourceCheckerMap;

    @Inject
    private Map<AuthorizationResourceType, ResourceBasedCrnProvider> resourceBasedCrnProviderMap;

    private AuthorizationMessageUtilsService authorizationMessageUtilsService;

    public boolean legacyAuthorizationNeeded() {
        return !entitlementService.isAuthorizationEntitlementRegistered(ThreadBasedUserCrnProvider.getAccountId());
    }

    public ResourceBasedCrnProvider getResourceBasedCrnProvider(AuthorizationResourceAction action) {
        AuthorizationResourceType resourceType = umsRightProvider.getResourceType(action);
        return resourceBasedCrnProviderMap.get(resourceType);
    }

    public void checkPermissionForUser(AuthorizationResourceAction action, String userCrn) {
        umsAccountAuthorizationService.checkRightOfUser(userCrn, action);
    }

    public void checkPermissionForUserOnResource(AuthorizationResourceAction action, String userCrn, String resourceCrn) {
        DefaultResourceChecker defaultResourceChecker = defaultResourceCheckerMap.get(umsRightProvider.getResourceType(action));
        if (defaultResourceChecker == null || !defaultResourceChecker.isDefault(resourceCrn)) {
            umsResourceAuthorizationService.checkRightOfUserOnResource(userCrn, action, resourceCrn);
        } else {
            throwAccessDeniedIfActionNotAllowed(action, List.of(resourceCrn), defaultResourceChecker);
        }
    }

    public void checkPermissionForUserOnResources(AuthorizationResourceAction action,
            String userCrn, Collection<String> resourceCrns) {
        DefaultResourceChecker defaultResourceChecker = defaultResourceCheckerMap.get(umsRightProvider.getResourceType(action));
        if (defaultResourceChecker == null) {
            umsResourceAuthorizationService.checkRightOfUserOnResources(userCrn, action, resourceCrns);
        } else {
            CrnsByCategory crnsByCategory = defaultResourceChecker.getDefaultResourceCrns(resourceCrns);
            if (!crnsByCategory.getDefaultResourceCrns().isEmpty()) {
                throwAccessDeniedIfActionNotAllowed(action, resourceCrns, defaultResourceChecker);
            }
            if (!crnsByCategory.getNotDefaultResourceCrns().isEmpty()) {
                umsResourceAuthorizationService.checkRightOfUserOnResources(userCrn, action, crnsByCategory.getNotDefaultResourceCrns());
            }
        }
    }

    public Map<String, Boolean> getPermissionsForUserOnResources(AuthorizationResourceAction action,
            String userCrn, List<String> resourceCrns) {
        DefaultResourceChecker defaultResourceChecker = defaultResourceCheckerMap.get(umsRightProvider.getResourceType(action));
        if (defaultResourceChecker == null) {
            return umsResourceAuthorizationService.getRightOfUserOnResources(userCrn, action, resourceCrns);
        } else {
            CrnsByCategory crnsByCategory = defaultResourceChecker.getDefaultResourceCrns(resourceCrns);
            Map<String, Boolean> result = new HashMap<>();
            if (!crnsByCategory.getDefaultResourceCrns().isEmpty()) {
                result.putAll(crnsByCategory.getDefaultResourceCrns().stream().collect(toMap(Function.identity(),
                        s -> defaultResourceChecker.isAllowedAction(action))));
            }
            if (!crnsByCategory.getNotDefaultResourceCrns().isEmpty()) {
                result.putAll(umsResourceAuthorizationService.getRightOfUserOnResources(userCrn, action, crnsByCategory.getNotDefaultResourceCrns()));
            }
            return result;
        }
    }

    public Object proceed(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime) {
        LOGGER.debug("Permission check took {} ms (method: {})", System.currentTimeMillis() - startTime,
                methodSignature.getMethod().getDeclaringClass().getSimpleName() + "#" + methodSignature.getMethod().getName());
        try {
            Object proceed = proceedingJoinPoint.proceed();
            if (proceed == null) {
                LOGGER.debug("Return value is null, method signature: {}", methodSignature.toLongString());
            }
            return proceed;
        } catch (Error | RuntimeException unchecked) {
            throw unchecked;
        } catch (Throwable t) {
            throw new AccessDeniedException(t.getMessage(), t);
        }
    }

    public <T> T getParameter(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, Class<? extends Annotation> annotation,
            Class<T> target) {
        List<Parameter> parameters = Lists.newArrayList(methodSignature.getMethod().getParameters());
        List<Parameter> matchingParameters = parameters.stream()
                .filter(parameter -> parameter.isAnnotationPresent(annotation))
                .collect(Collectors.toList());
        if (matchingParameters.size() != 1) {
            throw new IllegalStateException(String.format("Your controller method %s should have one and only one parameter with the annotation %s",
                    methodSignature.getMethod().getName(), annotation.getSimpleName()));
        }
        Object result = proceedingJoinPoint.getArgs()[parameters.indexOf(matchingParameters.iterator().next())];
        if (!target.isInstance(result)) {
            throw new IllegalStateException(
                    String.format("The type of the annotated parameter does not match with the expected type %s", target.getSimpleName()));
        }
        return (T) result;
    }

    public void throwAccessDeniedIfActionNotAllowed(AuthorizationResourceAction action, Collection<String> resourceCrns,
            DefaultResourceChecker defaultResourceChecker) {
        if (!defaultResourceChecker.isAllowedAction(action)) {
            String right = umsRightProvider.getRight(action);
            String unauthorizedMessage = authorizationMessageUtilsService.formatTemplate(right, resourceCrns);
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

}

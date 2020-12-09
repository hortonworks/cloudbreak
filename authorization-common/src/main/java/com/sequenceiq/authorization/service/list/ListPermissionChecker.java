package com.sequenceiq.authorization.service.list;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.resource.AuthorizationFilterableResponseCollection;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.ResourceCrnAwareApiModel;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class ListPermissionChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListPermissionChecker.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private EntitlementService entitlementService;

    public Object checkPermissions(FilterListBasedOnPermissions methodAnnotation, String userCrn,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, long startTime) {
        AuthorizationResourceAction action = methodAnnotation.action();
        if (commonPermissionCheckingUtils.legacyAuthorizationNeeded()) {
            commonPermissionCheckingUtils.checkPermissionForUser(action, userCrn);
            return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
        }
        if (entitlementService.listFilteringEnabled(Crn.safeFromString(userCrn).getAccountId())) {
            List<String> allResourceCrns = commonPermissionCheckingUtils.getResourceBasedCrnProvider(action).getResourceCrnsInAccount();
            Set<String> filteredResourceCrns = commonPermissionCheckingUtils.getPermissionsForUserOnResources(action, userCrn, allResourceCrns)
                    .entrySet()
                    .stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            Object result = commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
            switch (ListResponseFilteringType.getByClass(result.getClass())) {
                case SET:
                    return filterSet(filteredResourceCrns, (Set) result);
                case LIST:
                    return filterList(filteredResourceCrns, (List) result);
                case FILTERABLE_RESPONSE_MODEL:
                    return filterAuthorizationFilterableResponse(filteredResourceCrns, (AuthorizationFilterableResponseCollection) result);
                case UNSUPPORTED:
                default:
                    throw new IllegalStateException("Response of list API should be List, Set or an instance of " +
                            "AuthorizationFilterableResponseCollection interface");

            }
        } else {
            return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
        }
    }

    private Object filterAuthorizationFilterableResponse(Set<String> filteredResourceCrns, AuthorizationFilterableResponseCollection result) {
        AuthorizationFilterableResponseCollection authzResult = result;
        Collection<ResourceCrnAwareApiModel> authorizationFilterableResults = authzResult.getResponses();
        Set<ResourceCrnAwareApiModel> filtered = authorizationFilterableResults.stream()
                .filter(resourceCrnAwareApiModel -> filteredResourceCrns.contains(resourceCrnAwareApiModel.getResourceCrn()))
                .collect(Collectors.toSet());
        authzResult.setResponses(filtered);
        return authzResult;
    }

    private Object filterSet(Set<String> filteredResourceCrns, Set result) {
        return result.stream()
                .filter(getResourceCrnAwareApiModelPredicate(filteredResourceCrns))
                .collect(Collectors.toSet());
    }

    private Object filterList(Set<String> filteredResourceCrns, List result) {
        return result.stream()
                .filter(getResourceCrnAwareApiModelPredicate(filteredResourceCrns))
                .collect(Collectors.toList());
    }

    private Predicate getResourceCrnAwareApiModelPredicate(Set<String> filteredResourceCrns) {
        return resultObject -> {
            if (resultObject instanceof ResourceCrnAwareApiModel) {
                return filteredResourceCrns.contains(((ResourceCrnAwareApiModel) resultObject).getResourceCrn());
            } else {
                throw new IllegalStateException("Items of your response list or set should implement " +
                        "ResourceCrnAwareApiModel interface");
            }
        };
    }
}

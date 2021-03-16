package com.sequenceiq.authorization.service.list;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder;

public abstract class AbstractAuthorizationFiltering<T> {

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ResourceFilteringService resourceFilteringService;

    protected abstract List<ResourceWithId> getAllResources(Map<String, Object> args);

    protected abstract T filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args);

    protected abstract T getAll(Map<String, Object> args);

    public final T filterResources(Crn userCrn, AuthorizationResourceAction action, Map<String, Object> arguments) {
        T authorizedData;
        if (InternalCrnBuilder.isInternalCrn(userCrn) ||
                !userCrn.getResourceType().equals(Crn.ResourceType.USER) ||
                !entitlementService.listFilteringEnabled(userCrn.getAccountId())) {
            authorizedData = getAll(arguments);
        } else {
            List<ResourceWithId> resources = getAllResources(arguments);
            List<Long> authorizedResourceIds = getAuthorizedResourceIds(userCrn, action, resources);
            authorizedData = filterByIds(authorizedResourceIds, arguments);
        }
        return authorizedData;
    }

    private List<Long> getAuthorizedResourceIds(Crn userCrn, AuthorizationResourceAction action, List<ResourceWithId> resources) {
        return resourceFilteringService.filter(userCrn, action, resources, hasRightPredicate -> resources.stream()
                .filter(r -> hasRightPredicate.test(r.getResourceCrn()))
                .map(ResourceWithId::getId)
                .collect(Collectors.toList()));
    }
}

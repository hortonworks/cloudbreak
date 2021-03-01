package com.sequenceiq.authorization.service.list;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public abstract class AbstractAuthorizationFiltering<T> {

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    protected abstract List<AuthorizationResource> getAllResources(Map<String, Object> args);

    protected abstract T filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args);

    protected abstract T getAll(Map<String, Object> args);

    public final T filterResources(Crn userCrn, AuthorizationResourceAction action, Map<String, Object> arguments) {
        T authorizedData;
        if (InternalCrnBuilder.isInternalCrn(userCrn) ||
                !userCrn.getResourceType().equals(Crn.ResourceType.USER) ||
                !entitlementService.listFilteringEnabled(userCrn.getAccountId())) {
            authorizedData = getAll(arguments);
        } else {
            List<AuthorizationResource> resources = getAllResources(arguments);
            List<Long> authorizedResourceIds = getAuthorizedResourceIds(userCrn, action, resources, getRequestId());
            authorizedData = filterByIds(authorizedResourceIds, arguments);
        }
        return authorizedData;
    }

    private List<Long> getAuthorizedResourceIds(Crn userCrn, AuthorizationResourceAction action, List<AuthorizationResource> resources,
            Optional<String> requestId) {
        if (CollectionUtils.isEmpty(resources)) {
            return List.of();
        }
        Map<Optional<String>, List<AuthorizationResource>> resourcesByParents = sortByParents(resources);
        List<String> resourceCrns = flattenByParents(resourcesByParents);
        List<Boolean> umsResult = callUms(action, requestId, resourceCrns, userCrn);
        return evaluateUmsResult(umsResult, resourcesByParents);
    }

    private List<Boolean> callUms(AuthorizationResourceAction action, Optional<String> requestId, List<String> resourceCrns, Crn userCrn) {
        return grpcUmsClient.hasRightsOnResources(userCrn.toString(), userCrn.toString(), resourceCrns, action.getRight(), requestId);
    }

    private Map<Optional<String>, List<AuthorizationResource>> sortByParents(List<AuthorizationResource> resources) {
        Map<Optional<String>, List<AuthorizationResource>> resourcesByParents = new LinkedHashMap<>();
        resources.forEach(resource -> resourcesByParents
                .computeIfAbsent(resource.getParentResourceCrn(), ignored -> new ArrayList<>())
                .add(resource));
        return resourcesByParents;
    }

    private List<String> flattenByParents(Map<Optional<String>, List<AuthorizationResource>> resourcesByParents) {
        List<String> resourceCrns = new ArrayList<>();
        for (Map.Entry<Optional<String>, List<AuthorizationResource>> entry : resourcesByParents.entrySet()) {
            Optional<String> parentResource = entry.getKey();
            List<AuthorizationResource> subResources = entry.getValue();
            if (parentResource.isPresent()) {
                resourceCrns.add(parentResource.get());
            }
            resourceCrns.addAll(subResources
                    .stream()
                    .map(AuthorizationResource::getResourceCrn)
                    .collect(Collectors.toList()));
        }
        return resourceCrns;
    }

    private List<Long> evaluateUmsResult(List<Boolean> umsResult, Map<Optional<String>, List<AuthorizationResource>> resourcesByParents) {
        List<Long> authorizedResourceIds = new ArrayList<>();
        Iterator<Boolean> resultIterator = umsResult.iterator();
        for (Map.Entry<Optional<String>, List<AuthorizationResource>> entry : resourcesByParents.entrySet()) {
            Optional<String> parentResource = entry.getKey();
            List<AuthorizationResource> subResources = entry.getValue();
            if (parentResource.isPresent() && resultIterator.next()) {
                for (AuthorizationResource authorizationResource : subResources) {
                    resultIterator.next();
                    authorizedResourceIds.add(authorizationResource.getId());
                }
            } else {
                for (AuthorizationResource authorizationResource : subResources) {
                    if (resultIterator.next()) {
                        authorizedResourceIds.add(authorizationResource.getId());
                    }
                }
            }
        }
        return authorizedResourceIds;
    }

    private Optional<String> getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return Optional.of(requestId);
    }
}

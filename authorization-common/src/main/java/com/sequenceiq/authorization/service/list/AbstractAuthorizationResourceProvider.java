package com.sequenceiq.authorization.service.list;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

public abstract class AbstractAuthorizationResourceProvider implements ResourceListProvider {

    private static final int SELECT_IN_TRESHOLD = 10;

    protected abstract List<ResourceWithId> findByAccoundId(String accountId);

    protected abstract List<ResourceWithId> findByAccoundIdAndCrns(String accountId, List<String> resourceCrns);

    @Override
    public List<Resource> findResources(String accountId, List<String> resourceCrns) {
        if (CollectionUtils.isEmpty(resourceCrns)) {
            return List.of();
        }
        List<ResourceWithId> authorizationResources;
        if (resourceCrns.size() <= SELECT_IN_TRESHOLD) {
            authorizationResources = findByAccoundIdAndCrns(accountId, resourceCrns);
        } else {
            Set<String> crnsSet = new HashSet<>(resourceCrns);
            authorizationResources = findByAccoundId(accountId)
                    .stream()
                    .filter(a -> crnsSet.contains(a.getResourceCrn()))
                    .collect(Collectors.toList());
        }
        return authorizationResources.stream()
                .map(a -> new Resource(a.getResourceCrn(), a.getParentResourceCrn()))
                .collect(Collectors.toList());
    }
}

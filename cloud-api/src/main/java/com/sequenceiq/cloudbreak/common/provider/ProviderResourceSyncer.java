package com.sequenceiq.cloudbreak.common.provider;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public interface ProviderResourceSyncer<ResourceType> extends CloudPlatformAware  {

    List<CloudResourceStatus> sync(AuthenticatedContext authenticatedContext, List<CloudResource> resources);

    ResourceType getResourceType();

    default Set<String> getResourceListByType(List<CloudResource> resources) {
        return resources.stream()
                .filter(r -> r.getType() == getResourceType())
                .map(CloudResource::getReference)
                .collect(Collectors.toSet());
    }
}
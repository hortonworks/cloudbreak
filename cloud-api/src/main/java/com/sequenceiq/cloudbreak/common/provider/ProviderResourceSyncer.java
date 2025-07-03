package com.sequenceiq.cloudbreak.common.provider;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public interface ProviderResourceSyncer<ResourceType> extends CloudPlatformAware  {

    List<CloudResourceStatus> sync(AuthenticatedContext authenticatedContext, List<CloudResource> resources);

    ResourceType getResourceType();

    default Set<String> getResourceReferencesByType(List<CloudResource> resources) {
        return resources.stream()
                .filter(r -> r.getType() == getResourceType())
                .map(CloudResource::getReference)
                .collect(Collectors.toSet());
    }

    default Set<CloudResource> getResourcesByType(List<CloudResource> resources) {
        return resources.stream()
                .filter(r -> r.getType() == getResourceType())
                .collect(Collectors.toSet());
    }

    default Optional<CloudResource> getResourceByType(List<CloudResource> resources, com.sequenceiq.common.api.type.ResourceType resourceType) {
        return resources.stream()
                .filter(r -> r.getType() == resourceType)
                .findFirst();
    }

    default boolean shouldSync(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        return true;
    }
}
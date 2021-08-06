package com.sequenceiq.cloudbreak.cloud.service;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

public interface ResourceRetriever {

    Optional<CloudResource> findByResourceReferenceAndStatusAndType(String resourceReference, CommonStatus status, ResourceType resourceType);

    default Optional<CloudResource> findByResourceReferenceAndStatusAndTypeAndStack(String resourceReference, CommonStatus status, ResourceType resourceType,
            Long stackId) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default Optional<CloudResource> findFirstByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }
}

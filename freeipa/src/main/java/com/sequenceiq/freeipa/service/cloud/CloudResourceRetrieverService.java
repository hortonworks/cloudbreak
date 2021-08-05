package com.sequenceiq.freeipa.service.cloud;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Component
public class CloudResourceRetrieverService implements ResourceRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceRetrieverService.class);

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private ResourceService resourceService;

    @Override
    public Optional<CloudResource> findByResourceReferenceAndStatusAndType(String resourceReference, CommonStatus status, ResourceType resourceType) {
        Optional<Resource> optionalResource = resourceService.findByResourceReferenceAndStatusAndType(resourceReference, status, resourceType);
        LOGGER.debug("Resource retrieved by optionalResource reference: {}, status: {} and type: {}. Is present: {}", resourceReference, status, resourceType,
                optionalResource.isPresent());
        return optionalResource
                .map(resource -> conversionService.convert(resource, CloudResource.class));
    }

    @Override
    public Optional<CloudResource> findByResourceReferenceAndStatusAndTypeAndStack(String resourceReference,
        CommonStatus status, ResourceType resourceType, Long stackId) {
        Optional<Resource> optionalResource = resourceService.findByResourceReferenceAndStatusAndTypeAndStack(resourceReference, status, resourceType, stackId);
        LOGGER.debug("Resource retrieved by optionalResource reference: {}, status: {}, type: {}, stackId: {}. Is present: {}", resourceReference, status,
                resourceType, stackId, optionalResource.isPresent());
        return optionalResource
                .map(resource -> conversionService.convert(resource, CloudResource.class));
    }
}

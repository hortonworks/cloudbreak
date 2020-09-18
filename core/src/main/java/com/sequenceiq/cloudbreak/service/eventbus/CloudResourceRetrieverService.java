package com.sequenceiq.cloudbreak.service.eventbus;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

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
}

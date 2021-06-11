package com.sequenceiq.environment.resourcepersister;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class CloudResourceRetrieverService implements ResourceRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceRetrieverService.class);

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private ResourceService resourceService;

    @Override
    public Optional<CloudResource> findByResourceReferenceAndStatusAndType(String resourceReference, CommonStatus status, ResourceType resourceType) {
        Optional<Resource> optionalResource = resourceService.findByResourceReferenceAndStatusAndType(resourceReference, status, resourceType);
        LOGGER.debug("Resource retrieved by optionalResource reference: {}, status: {} and type: {}. Is present: {}", resourceReference, status, resourceType,
                optionalResource.isPresent());
        return optionalResource
                .map(resource -> cloudResourceConverter.convert(resource));
    }

    public Optional<CloudResource> findByEnvironmentIdAndType(Long environmentId, ResourceType resourceType) {
        Optional<Resource> optionalResource = resourceService.findByEnvironmentIdAndType(environmentId, resourceType);
        LOGGER.debug("Resource retrieved by environmentId: {} and type: {}. Is present: {}", environmentId, resourceType, optionalResource.isPresent());
        return optionalResource
                .map(resource -> cloudResourceConverter.convert(resource));
    }

}

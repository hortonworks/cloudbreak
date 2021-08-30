package com.sequenceiq.cloudbreak.service.eventbus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
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

    @Override
    public Optional<CloudResource> findByResourceReferenceAndStatusAndTypeAndStack(String resourceReference, CommonStatus status, ResourceType resourceType,
            Long stackId) {
        Optional<Resource> optionalResource = resourceService.findByResourceReferenceAndStatusAndTypeAndStack(resourceReference, status, resourceType, stackId);
        LOGGER.debug("Resource retrieved by optionalResource reference: {}, status: {}, type: {}, stackId: {}. Is present: {}", resourceReference, status,
                resourceType, stackId, optionalResource.isPresent());
        return optionalResource
                .map(resource -> cloudResourceConverter.convert(resource));
    }

    @Override
    public Optional<CloudResource> findFirstByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        Optional<Resource> optionalResource = resourceService.findFirstByStatusAndTypeAndStack(status, resourceType, stackId);
        LOGGER.debug("Resource retrieved by optionalResource status: {}, type: {}, stackId: {}. Is present: {}", status, resourceType, stackId,
                optionalResource.isPresent());
        return optionalResource
                .map(resource -> cloudResourceConverter.convert(resource));
    }

    @Override
    public List<CloudResource> findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus status, ResourceType resourceType, Long stackId,
            String instanceGroup) {
        List<Resource> resources = resourceService.findAllByResourceStatusAndResourceTypeAndStackIdAndInstanceGroup(status, resourceType, stackId,
                instanceGroup);
        return resources.stream().map(resource -> cloudResourceConverter.convert(resource)).collect(Collectors.toList());
    }
}

package com.sequenceiq.freeipa.service.cloud;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Component
public class CloudResourceRetrieverService implements ResourceRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceRetrieverService.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Override
    public Optional<CloudResource> findByResourceReferenceAndStatusAndType(String resourceReference, CommonStatus status, ResourceType resourceType) {
        Optional<Resource> optionalResource = resourceService.findByResourceReferenceAndStatusAndType(resourceReference, status, resourceType);
        LOGGER.debug("Resource retrieved by optionalResource reference: {}, status: {} and type: {}. Is present: {}", resourceReference, status, resourceType,
                optionalResource.isPresent());
        return optionalResource
                .map(resource -> resourceToCloudResourceConverter.convert(resource));
    }

    @Override
    public Optional<CloudResource> findByResourceReferenceAndStatusAndTypeAndStack(String resourceReference,
        CommonStatus status, ResourceType resourceType, Long stackId) {
        Optional<Resource> optionalResource = resourceService.findByResourceReferenceAndStatusAndTypeAndStack(resourceReference, status, resourceType, stackId);
        LOGGER.debug("Resource retrieved by optionalResource reference: {}, status: {}, type: {}, stackId: {}. Is present: {}", resourceReference, status,
                resourceType, stackId, optionalResource.isPresent());
        return optionalResource
                .map(resource -> resourceToCloudResourceConverter.convert(resource));
    }

    @Override
    public List<CloudResource> findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus status, ResourceType resourceType, Long stackId,
            String instanceGroup) {
        return findAllByStatusAndTypeAndStack(status, resourceType, stackId);
    }

    @Override
    public List<CloudResource> findAllByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        List<Resource> resources = resourceService.findAllByResourceStatusAndResourceTypeAndStackId(status, resourceType, stackId);
        return resources.stream().map(resource -> resourceToCloudResourceConverter.convert(resource)).collect(Collectors.toList());
    }
}

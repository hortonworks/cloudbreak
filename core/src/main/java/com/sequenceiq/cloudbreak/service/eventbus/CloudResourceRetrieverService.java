package com.sequenceiq.cloudbreak.service.eventbus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

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
    public List<CloudResource> findByResourceReferencesAndStatusAndType(List<String> resourceReferences, CommonStatus status, ResourceType resourceType) {
        List<Resource> resources = resourceService.findByResourceReferencesAndStatusAndType(resourceReferences, status, resourceType);
        LOGGER.debug("Resource retrieved by resource references({}): {}, status: {} and type: {}. Retrieved size: {}", resourceReferences.size(),
                resourceReferences, status, resourceType, resources.size());
        return resources
                .stream()
                .map(resource -> cloudResourceConverter.convert(resource))
                .collect(Collectors.toList());
    }

    @Override
    public List<CloudResource> findByResourceReferencesAndStatusAndTypeAndStack(List<String> resourceReferences, CommonStatus status, ResourceType resourceType,
            Long stackId) {
        List<Resource> resources = resourceService.findByResourceReferencesAndStatusAndTypeAndStack(resourceReferences, status, resourceType, stackId);
        LOGGER.debug("Resource retrieved by resource references({}): {}, status: {} and type: {}, stackId: {}. Retrieved size: {}", resourceReferences.size(),
                resourceReferences, status, resourceType, stackId, resources.size());
        return resources
                .stream()
                .map(resource -> cloudResourceConverter.convert(resource))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CloudResource> findByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
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

    @Override
    public List<CloudResource> findAllByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        List<Resource> resources = resourceService.findAllByResourceStatusAndResourceTypeAndStackId(status, resourceType, stackId);
        return resources.stream().map(resource -> cloudResourceConverter.convert(resource)).collect(Collectors.toList());
    }
}

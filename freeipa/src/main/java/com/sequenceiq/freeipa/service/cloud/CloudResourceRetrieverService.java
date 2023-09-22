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
    public List<CloudResource> findByResourceReferencesAndStatusAndType(List<String> resourceReferences, CommonStatus status, ResourceType resourceType) {
        List<Resource> resources = resourceService.findByResourceReferencesAndStatusAndType(resourceReferences, status, resourceType);
        LOGGER.debug("Resource retrieved by resource references({}): {}, status: {} and type: {}. Retrieved size: {}", resourceReferences.size(),
                resourceReferences, status, resourceType, resources.size());
        return resources.stream()
                .map(resource -> resourceToCloudResourceConverter.convert(resource))
                .collect(Collectors.toList());
    }

    @Override
    public List<CloudResource> findByResourceReferencesAndStatusAndTypeAndStack(List<String> resourceReferences, CommonStatus status, ResourceType resourceType,
            Long stackId) {
        List<Resource> resources = resourceService.findByResourceReferencesAndStatusAndTypeAndStack(resourceReferences, status, resourceType, stackId);
        LOGGER.debug("Resource retrieved by resource references({}): {}, status: {} and type: {}, stackId: {}. Retrieved size: {}", resourceReferences.size(),
                resourceReferences, status, resourceType, stackId, resources.size());
        return resources.stream()
                .map(resource -> resourceToCloudResourceConverter.convert(resource))
                .collect(Collectors.toList());
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

    @Override
    public Optional<CloudResource> findByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        Optional<Resource> optionalResource = resourceService.findFirstByStatusAndTypeAndStack(status, resourceType, stackId);
        LOGGER.debug("Resource retrieved by optionalResource status: {}, type: {}, stackId: {}. Is present: {}", status, resourceType, stackId,
                optionalResource.isPresent());
        return optionalResource.map(resource -> resourceToCloudResourceConverter.convert(resource));
    }
}

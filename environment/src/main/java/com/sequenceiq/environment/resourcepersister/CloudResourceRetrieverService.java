package com.sequenceiq.environment.resourcepersister;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

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
    public List<CloudResource> findByResourceReferencesAndStatusAndType(List<String> resourceReferences, CommonStatus status, ResourceType resourceType) {
        List<Resource> resources = resourceService.findByResourceReferencesAndStatusAndType(resourceReferences, status, resourceType);
        LOGGER.debug("Resource retrieved by resource reference ({}): {}, status: {} and type: {}. Is present: {}", resourceReferences.size(),
                resourceReferences, status, resourceType, resources.size());
        return resources
                .stream()
                .map(resource -> cloudResourceConverter.convert(resource))
                .collect(Collectors.toList());
    }

    public Optional<CloudResource> findByEnvironmentIdAndType(Long environmentId, ResourceType resourceType) {
        Optional<Resource> optionalResource = resourceService.findByEnvironmentIdAndType(environmentId, resourceType);
        LOGGER.debug("Resource retrieved by environmentId: {} and type: {}. Is present: {}", environmentId, resourceType, optionalResource.isPresent());
        return optionalResource
                .map(resource -> cloudResourceConverter.convert(resource));
    }

}

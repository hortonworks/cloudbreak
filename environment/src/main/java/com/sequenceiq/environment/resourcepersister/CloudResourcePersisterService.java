package com.sequenceiq.environment.resourcepersister;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Component
public class CloudResourcePersisterService implements Persister<ResourceNotification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private CloudResourceToResourceConverter cloudResourceToResourceConverter;

    @Override
    public ResourceNotification persist(ResourceNotification notification) {
        LOGGER.debug("Resource allocation notification received: {}", notification);
        Long envId = notification.getCloudContext().getId();
        CloudResource cloudResource = notification.getCloudResource();
        Resource resource = cloudResourceToResourceConverter.convert(cloudResource);
        Optional<Resource> persistedResource =
                resourceService.findByResourceReferenceAndType(cloudResource.getReference(), cloudResource.getType());
        if (persistedResource.isPresent()) {
            LOGGER.debug("Trying to persist a resource (name: {}, type: {}, environmentId: {}) that is already persisted, skipping..",
                    cloudResource.getName(), cloudResource.getType().name(), envId);
            return notification;
        }
        resource.setEnvironment(environmentService.findEnvironmentByIdOrThrow(envId));
        resourceService.save(resource);
        return notification;
    }

    @Override
    public ResourceNotification update(ResourceNotification notification) {
        LOGGER.debug("Resource update notification received: {}", notification);
        Long envId = notification.getCloudContext().getId();
        CloudResource cloudResource = notification.getCloudResource();
        Resource persistedResource = resourceService.findByResourceReferenceAndType(cloudResource.getReference(),
                ResourceType.valueOf(cloudResource.getType().name())).orElseThrow(NotFoundException.notFound("resource", cloudResource.getName()));
        Resource resource = cloudResourceToResourceConverter.convert(cloudResource);
        updateWithPersistedFields(resource, persistedResource);
        resource.setEnvironment(environmentService.findEnvironmentByIdOrThrow(envId));
        resourceService.save(resource);
        return notification;
    }

    @Override
    public ResourceNotification delete(ResourceNotification notification) {
        LOGGER.debug("Resource deletion notification received: {}", notification);
        CloudResource cloudResource = notification.getCloudResource();
        resourceService.findByResourceReferenceAndType(cloudResource.getReference(),
                ResourceType.valueOf(cloudResource.getType().name())).ifPresent(value -> resourceService.delete(value));
        return notification;
    }

    private void updateWithPersistedFields(Resource resource, Resource persistedResource) {
        if (persistedResource != null) {
            resource.setId(persistedResource.getId());
        }
    }
}

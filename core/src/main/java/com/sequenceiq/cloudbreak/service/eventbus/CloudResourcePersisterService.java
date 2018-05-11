package com.sequenceiq.cloudbreak.service.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class CloudResourcePersisterService extends AbstractCloudPersisterService<ResourceNotification> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Override
    public ResourceNotification persist(ResourceNotification notification) {
        LOGGER.debug("Resource allocation notification received: {}", notification);
        Long stackId = notification.getCloudContext().getId();
        CloudResource cloudResource = notification.getCloudResource();
        Resource resource = getConversionService().convert(cloudResource, Resource.class);
        ResourceRepository resourceRepository = getResourceRepository();
        Resource persistedResource = resourceRepository.findByStackIdAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType());
        if (persistedResource != null) {
            LOGGER.warn("Trying to persist a resource (name: {}, type: {}, stackId: {}) that is already persisted, skipping..",
                    cloudResource.getName(), cloudResource.getType().name(), stackId);
            return notification;
        }
        resource.setStack(getStackRepository().findOne(stackId));
        resourceRepository.save(resource);
        return notification;
    }

    @Override
    public ResourceNotification update(ResourceNotification notification) {
        LOGGER.debug("Resource update notification received: {}", notification);
        Long stackId = notification.getCloudContext().getId();
        CloudResource cloudResource = notification.getCloudResource();
        ResourceRepository repository = getResourceRepository();
        Resource persistedResource = repository.findByStackIdAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType());
        Resource resource = getConversionService().convert(cloudResource, Resource.class);
        updateWithPersistedFields(resource, persistedResource);
        resource.setStack(getStackRepository().findOne(stackId));
        repository.save(resource);
        return notification;
    }

    @Override
    public ResourceNotification delete(ResourceNotification notification) {
        LOGGER.debug("Resource deletion notification received: {}", notification);
        Long stackId = notification.getCloudContext().getId();
        CloudResource cloudResource = notification.getCloudResource();
        ResourceRepository repository = getResourceRepository();
        Resource resource = repository.findByStackIdAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType());
        if (resource != null) {
            repository.delete(resource);
        }
        return notification;
    }

    @Override
    public ResourceNotification retrieve(ResourceNotification data) {
        return null;
    }

    private StackRepository getStackRepository() {
        return getRepositoryForEntity(Stack.class);
    }

    private ResourceRepository getResourceRepository() {
        return getRepositoryForEntity(Resource.class);
    }

    private void updateWithPersistedFields(Resource resource, Resource persistedResource) {
        if (persistedResource != null) {
            resource.setId(persistedResource.getId());
            resource.setInstanceGroup(persistedResource.getInstanceGroup());
        }
    }
}

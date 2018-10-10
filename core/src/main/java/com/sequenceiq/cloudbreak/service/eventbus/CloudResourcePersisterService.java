package com.sequenceiq.cloudbreak.service.eventbus;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CrudRepositoryLookupService;

@Component
public class CloudResourcePersisterService implements Persister<ResourceNotification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private CrudRepositoryLookupService repositoryLookupService;

    @Override
    public ResourceNotification persist(ResourceNotification notification) {
        LOGGER.debug("Resource allocation notification received: {}", notification);
        Long stackId = notification.getCloudContext().getId();
        CloudResource cloudResource = notification.getCloudResource();
        Resource resource = conversionService.convert(cloudResource, Resource.class);
        ResourceRepository resourceRepository = getResourceRepository();
        Resource persistedResource = resourceRepository.findByStackIdAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType());
        if (persistedResource != null) {
            LOGGER.warn("Trying to persist a resource (name: {}, type: {}, stackId: {}) that is already persisted, skipping..",
                    cloudResource.getName(), cloudResource.getType().name(), stackId);
            return notification;
        }
        resource.setStack(getStackRepository(stackId));
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
        Resource resource = conversionService.convert(cloudResource, Resource.class);
        updateWithPersistedFields(resource, persistedResource);
        resource.setStack(getStackRepository(stackId));
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

    private Stack getStackRepository(Long stackId) {
        StackRepository stackRepository = repositoryLookupService.getRepositoryForEntity(Stack.class);
        return stackRepository.findById(stackId).orElseThrow(notFound("Stack", stackId));
    }

    private ResourceRepository getResourceRepository() {
        return repositoryLookupService.getRepositoryForEntity(Resource.class);
    }

    private void updateWithPersistedFields(Resource resource, Resource persistedResource) {
        if (persistedResource != null) {
            resource.setId(persistedResource.getId());
            resource.setInstanceGroup(persistedResource.getInstanceGroup());
        }
    }
}

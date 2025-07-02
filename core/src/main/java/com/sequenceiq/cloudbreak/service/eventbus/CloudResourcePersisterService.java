package com.sequenceiq.cloudbreak.service.eventbus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.cloudbreak.converter.spi.CloudResourceToResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.StackReferenceRepository;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;

@Component
public class CloudResourcePersisterService implements Persister<ResourceNotification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Inject
    private StackReferenceRepository stackReferenceRepository;

    @Inject
    private ResourceService resourceService;

    @Inject
    private CloudResourceToResourceConverter cloudResourceToResourceConverter;

    @Override
    public ResourceNotification persist(ResourceNotification notification) {
        LOGGER.debug("Resource allocation notification received: {}", notification);
        Long stackId = notification.getCloudContext().getId();
        Stack stack = findStackById(stackId);
        List<CloudResource> cloudResources = notification.getCloudResources();
        List<Resource> resources = cloudResources.stream()
                .filter(cr -> !resourceExists(stackId, cr))
                .map(cloudResource -> {
                    Resource resource = cloudResourceToResourceConverter.convert(cloudResource);
                    setStack(stack, cloudResource, resource);
                    resourceService.save(resource);
                    return resource;
                })
                .toList();
        if (cloudResources.size() != resources.size()) {
            LOGGER.debug("There are {} resource(s) which won't be saved", cloudResources.size() - resources.size());
        }
        return notification;
    }

    @Override
    public ResourceNotification update(ResourceNotification notification) {
        LOGGER.debug("Resource update notification received: {}", notification);
        Long stackId = notification.getCloudContext().getId();
        Stack stack = findStackById(stackId);
        List<CloudResource> cloudResources = notification.getCloudResources();
        cloudResources.forEach(cloudResource -> {
            Resource resource = cloudResourceToResourceConverter.convert(cloudResource);
            Resource persistedResource = getPersistedResource(stackId, cloudResource)
                    .orElseGet(() -> {
                        LOGGER.debug("Resource {} not found in DB, creating a new one", cloudResource.getName());
                        return resource;
                    });
            updateWithPersistedFields(resource, persistedResource);
            setStack(stack, cloudResource, resource);
            resourceService.save(resource);
        });
        return notification;
    }

    @Override
    public ResourceNotification delete(ResourceNotification notification) {
        LOGGER.debug("Resource deletion notification received: {}", notification);
        Long stackId = notification.getCloudContext().getId();
        List<CloudResource> cloudResources = notification.getCloudResources();
        AtomicInteger deleted = new AtomicInteger(0);
        cloudResources.stream()
                .filter(cr -> resourceExists(stackId, cr))
                .forEach(cloudResource -> {
                    deleteResource(stackId, cloudResource);
                    deleted.incrementAndGet();
                });
        LOGGER.debug("There are {} deleted resource(s)", deleted.get());
        return notification;
    }

    private Stack findStackById(Long stackId) {
        return stackReferenceRepository.getOne(stackId);
    }

    private void updateWithPersistedFields(Resource resource, Resource persistedResource) {
        if (persistedResource != null && !persistedResource.equals(resource)) {
            resource.setId(persistedResource.getId());
            resource.setInstanceGroup(persistedResource.getInstanceGroup());
        }
    }

    private Optional<Resource> getPersistedResource(Long stackId, CloudResource cloudResource) {
        if (cloudResource.isStackAware()) {
            return resourceService.findByStackIdAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType());
        } else {
            return resourceService.findByResourceReferenceAndType(cloudResource.getReference(), cloudResource.getType());
        }
    }

    private boolean resourceExists(Long stackId, CloudResource cloudResource) {
        boolean exists;
        String id;
        if (cloudResource.isStackAware()) {
            exists = resourceService.existsByStackIdAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType());
            id = cloudResource.getName();
        } else {
            exists = resourceService.existsByResourceReferenceAndType(cloudResource.getReference(), cloudResource.getType());
            id = cloudResource.getReference();
        }
        if (exists) {
            LOGGER.debug("{} and {} already exists in DB for stack.", id, cloudResource.getType());
        }
        return exists;
    }

    private void deleteResource(Long stackId, CloudResource cloudResource) {
        if (cloudResource.isStackAware()) {
            resourceService.deleteByStackIdAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType());
        } else {
            resourceService.deleteByResourceReferenceAndType(cloudResource.getReference(), cloudResource.getType());
        }
    }

    private void setStack(Stack stack, CloudResource cloudResource, Resource resource) {
        if (cloudResource.isStackAware()) {
            LOGGER.debug("Setting stack {} for cloud resource {} and type {}", stack.getId(), cloudResource.getName(), cloudResource.getType());
            resource.setStack(stack);
        }
    }
}

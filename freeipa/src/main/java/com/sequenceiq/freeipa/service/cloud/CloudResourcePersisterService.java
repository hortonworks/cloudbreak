package com.sequenceiq.freeipa.service.cloud;

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
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.converter.cloud.CloudResourceToResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class CloudResourcePersisterService implements Persister<ResourceNotification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackService stackService;

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
            LOGGER.debug("There are {} resource(s), these will not be save", cloudResources.size() - resources.size());
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
            Resource persistedResource = getPersistedResource(stackId, cloudResource)
                    .orElseThrow(NotFoundException.notFound("resource", cloudResource.getName()));
            Resource resource = cloudResourceToResourceConverter.convert(cloudResource);
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
                    resourceService.deleteByStackIdAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType());
                    deleted.incrementAndGet();
                });
        LOGGER.debug("There are {} deleted resource(s)", deleted.get());
        return notification;
    }

    private Stack findStackById(Long stackId) {
        return stackService.getStackById(stackId);
    }

    private void updateWithPersistedFields(Resource resource, Resource persistedResource) {
        if (persistedResource != null) {
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

    private void setStack(Stack stack, CloudResource cloudResource, Resource resource) {
        if (cloudResource.isStackAware()) {
            LOGGER.debug("Setting stack {} for cloud resource {} and type {}", stack.getId(), cloudResource.getName(), cloudResource.getType());
            resource.setStack(stack);
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
}
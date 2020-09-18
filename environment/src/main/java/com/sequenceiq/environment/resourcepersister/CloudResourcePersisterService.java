package com.sequenceiq.environment.resourcepersister;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;

@Component
public class CloudResourcePersisterService implements Persister<ResourceNotification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private ResourceService resourceService;

    @Override
    public ResourceNotification persist(ResourceNotification notification) {
        LOGGER.debug("Resource allocation notification received: {}", notification);
        String crn = notification.getCloudContext().getName();
        CloudResource cloudResource = notification.getCloudResource();
        Resource resource = conversionService.convert(cloudResource, Resource.class);
        Optional<Resource> persistedResource = resourceService.findByStackIdAndNameAndType(crn, cloudResource.getName(),
                ResourceType.valueOf(cloudResource.getType().name()));
        if (persistedResource.isPresent()) {
            LOGGER.debug("Trying to persist a resource (name: {}, type: {}, stackId: {}) that is already persisted, skipping..",
                    cloudResource.getName(), cloudResource.getType().name(), crn);
            return notification;
        }
        resource.setCrn(notification.getCloudResource().getName());
        resourceService.save(resource);
        return notification;
    }

    @Override
    public ResourceNotification update(ResourceNotification notification) {
        LOGGER.debug("Resource update notification received: {}", notification);
        String crn = notification.getCloudContext().getName();
        CloudResource cloudResource = notification.getCloudResource();
        Resource persistedResource = resourceService.findByStackIdAndNameAndType(crn, cloudResource.getName(),
                ResourceType.valueOf(cloudResource.getType().name())).orElseThrow(NotFoundException.notFound("resource", cloudResource.getName()));
        Resource resource = conversionService.convert(cloudResource, Resource.class);
        updateWithPersistedFields(resource, persistedResource);
        resource.setCrn(notification.getCloudResource().getName());
        resourceService.save(resource);
        return notification;
    }

    @Override
    public ResourceNotification delete(ResourceNotification notification) {
        LOGGER.debug("Resource deletion notification received: {}", notification);
        String crn = notification.getCloudContext().getName();
        CloudResource cloudResource = notification.getCloudResource();
        resourceService.findByStackIdAndNameAndType(crn, cloudResource.getName(), ResourceType.valueOf(cloudResource.getType().name()))
                .ifPresent(value -> resourceService.delete(value));
        return notification;
    }

    private void updateWithPersistedFields(Resource resource, Resource persistedResource) {
        if (persistedResource != null) {
            resource.setId(persistedResource.getId());
            resource.setInstanceGroup(persistedResource.getInstanceGroup());
        }
    }

}

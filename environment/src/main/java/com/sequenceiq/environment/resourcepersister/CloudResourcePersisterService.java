package com.sequenceiq.environment.resourcepersister;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.environment.environment.domain.Environment;
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
        Environment environment = environmentService.findEnvironmentByIdOrThrow(envId);
        List<CloudResource> cloudResources = notification.getCloudResources();
        List<Resource> resources = cloudResources.stream()
                .filter(cr -> {
                    boolean exist = resourceService.existsByResourceReferenceAndType(cr.getReference(), cr.getType());
                    if (exist) {
                        LOGGER.debug("{} and {} already exists in DB for stack.", cr.getReference(), cr.getType());
                    }
                    return !exist;
                })
                .map(cloudResource -> {
                    Resource resource = cloudResourceToResourceConverter.convert(cloudResource);
                    resource.setEnvironment(environment);
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
        Long envId = notification.getCloudContext().getId();
        Environment environment = environmentService.findEnvironmentByIdOrThrow(envId);
        List<CloudResource> cloudResources = notification.getCloudResources();
        cloudResources.forEach(cloudResource -> {
            Resource resource = cloudResourceToResourceConverter.convert(cloudResource);
            Resource persistedResource = resourceService.findByResourceReferenceAndType(cloudResource.getReference(), cloudResource.getType())
                    .orElseGet(() -> {
                        LOGGER.debug("Resource {} not found in DB, creating a new one", cloudResource.getName());
                        return resource;
                    });
            resource.setEnvironment(environment);
            updateWithPersistedFields(resource, persistedResource);
            resourceService.save(resource);
        });
        return notification;
    }

    @Override
    public ResourceNotification delete(ResourceNotification notification) {
        LOGGER.debug("Resource deletion notification received: {}", notification);
        List<CloudResource> cloudResources = notification.getCloudResources();
        AtomicInteger deleted = new AtomicInteger(0);
        cloudResources.stream()
                .filter(cr -> resourceService.existsByResourceReferenceAndType(cr.getReference(), cr.getType()))
                .forEach(cloudResource -> {
                    resourceService.deleteByReferenceAndType(cloudResource.getReference(), cloudResource.getType());
                    deleted.incrementAndGet();
                });
        LOGGER.debug("There are {} deleted resource(s)", deleted.get());
        return notification;
    }

    private void updateWithPersistedFields(Resource resource, Resource persistedResource) {
        if (persistedResource != null && !persistedResource.equals(resource)) {
            resource.setId(persistedResource.getId());
        }
    }
}

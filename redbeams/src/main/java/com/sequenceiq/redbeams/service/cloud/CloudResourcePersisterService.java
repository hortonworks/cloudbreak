package com.sequenceiq.redbeams.service.cloud;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.redbeams.converter.stack.CloudResourceToDbResourceConverter;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBResourceService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class CloudResourcePersisterService implements Persister<ResourceNotification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Inject
    private DBResourceService dbResourceService;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private CloudResourceToDbResourceConverter cloudResourceToDbResourceConverter;

    @Override
    public ResourceNotification persist(ResourceNotification notification) {
        LOGGER.debug("DBResource allocation notification received: {}", notification);
        Long dbStackId = notification.getCloudContext().getId();
        DBStack dbStack = dbStackService.getById(dbStackId);
        List<CloudResource> cloudResources = notification.getCloudResources();
        List<DBResource> resources = cloudResources.stream()
                .filter(cr -> {
                    boolean exist = dbResourceService.existsByStackAndNameAndType(dbStackId, cr.getName(), cr.getType());
                    if (exist) {
                        LOGGER.debug("{} and {} already exists in DB for stack.", cr.getName(), cr.getType());
                    }
                    return !exist;
                })
                .map(cloudResource -> {
                    DBResource resource = cloudResourceToDbResourceConverter.convert(cloudResource);
                    resource.setDbStack(dbStack);
                    dbResourceService.save(resource);
                    return resource;
                })
                .collect(Collectors.toList());
        if (cloudResources.size() != resources.size()) {
            LOGGER.debug("There are {} resource(s), these will not be save", cloudResources.size() - resources.size());
        }
        return notification;
    }

    @Override
    public ResourceNotification update(ResourceNotification notification) {
        LOGGER.debug("DBResource update notification received: {}", notification);
        Long dbStackId = notification.getCloudContext().getId();
        DBStack dbStack = dbStackService.getById(dbStackId);
        List<CloudResource> cloudResources = notification.getCloudResources();
        cloudResources.forEach(cloudResource -> {
            DBResource resource = cloudResourceToDbResourceConverter.convert(cloudResource);
            DBResource persistedResource = dbResourceService.findByStackAndNameAndType(dbStackId, cloudResource.getName(), cloudResource.getType())
                    .orElseThrow(notFound("dbResource", cloudResource.getName()));
            updateWithPersistedFields(resource, persistedResource);
            resource.setDbStack(dbStack);
            dbResourceService.save(resource);
        });
        return notification;
    }

    @Override
    public ResourceNotification delete(ResourceNotification notification) {
        LOGGER.debug("DBResource deletion notification received: {}", notification);
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

    private void updateWithPersistedFields(DBResource resource, DBResource persistedResource) {
        if (persistedResource != null) {
            resource.setId(persistedResource.getId());
        }
    }

    private boolean resourceExists(Long stackId, CloudResource cloudResource) {
        boolean exists = dbResourceService.existsByStackAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType());
        String id = cloudResource.getName();
        if (exists) {
            LOGGER.debug("{} and {} already exists in DB for stack.", id, cloudResource.getType());
        }
        return exists;
    }

    private void deleteResource(Long stackId, CloudResource cloudResource) {
        dbResourceService.findByStackAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType())
                .ifPresent(value -> dbResourceService.delete(value));
    }
}

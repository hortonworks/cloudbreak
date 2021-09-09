package com.sequenceiq.redbeams.service.cloud;

import static com.sequenceiq.redbeams.exception.NotFoundException.notFound;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.redbeams.converter.stack.CloudResourceToDbResourceConverter;
import com.sequenceiq.redbeams.domain.stack.DBResource;
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
        CloudResource cloudResource = notification.getCloudResource();
        DBResource resource = cloudResourceToDbResourceConverter.convert(cloudResource);
        Optional<DBResource> persistedResource = dbResourceService.findByStackAndNameAndType(dbStackId, cloudResource.getName(), cloudResource.getType());
        if (persistedResource.isPresent()) {
            LOGGER.debug("Trying to persist a resource (name: {}, type: {}, stackId: {}) that is already persisted, skipping..",
                    cloudResource.getName(), cloudResource.getType().name(), dbStackId);
            return notification;
        }
        resource.setDbStack(dbStackService.getById(dbStackId));
        dbResourceService.save(resource);
        return notification;
    }

    @Override
    public ResourceNotification update(ResourceNotification notification) {
        LOGGER.debug("DBResource update notification received: {}", notification);
        Long dbStackId = notification.getCloudContext().getId();
        CloudResource cloudResource = notification.getCloudResource();
        DBResource persistedResource = dbResourceService.findByStackAndNameAndType(dbStackId, cloudResource.getName(), cloudResource.getType())
                .orElseThrow(notFound("dbResource", cloudResource.getName()));
        DBResource resource = cloudResourceToDbResourceConverter.convert(cloudResource);
        updateWithPersistedFields(resource, persistedResource);
        resource.setDbStack(dbStackService.getById(dbStackId));
        dbResourceService.save(resource);
        return notification;
    }

    @Override
    public ResourceNotification delete(ResourceNotification notification) {
        LOGGER.debug("DBResource deletion notification received: {}", notification);
        Long stackId = notification.getCloudContext().getId();
        CloudResource cloudResource = notification.getCloudResource();
        dbResourceService.findByStackAndNameAndType(stackId, cloudResource.getName(), cloudResource.getType())
                .ifPresent(value -> dbResourceService.delete(value));
        return notification;
    }

    private void updateWithPersistedFields(DBResource resource, DBResource persistedResource) {
        if (persistedResource != null) {
            resource.setId(persistedResource.getId());
        }
    }
}

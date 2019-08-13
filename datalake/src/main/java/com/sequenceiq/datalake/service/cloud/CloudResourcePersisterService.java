package com.sequenceiq.datalake.service.cloud;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

@Component
public class CloudResourcePersisterService implements Persister<ResourceNotification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public ResourceNotification persist(ResourceNotification notification) {
        LOGGER.debug("Resource allocation notification received: {}", notification);
        return notification;
    }

    @Override
    public ResourceNotification update(ResourceNotification notification) {
        LOGGER.debug("Resource update notification received: {}", notification);
        return notification;
    }

    @Override
    public ResourceNotification delete(ResourceNotification notification) {
        LOGGER.debug("Resource deletion notification received: {}", notification);
        return notification;
    }

    @Override
    public ResourceNotification retrieve(ResourceNotification data) {
        return null;
    }
}

package com.sequenceiq.consumption.service.cloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

@Component
public class CloudResourcePersisterService implements Persister<ResourceNotification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Override
    public ResourceNotification persist(ResourceNotification notification) {
        return null;
    }

    @Override
    public ResourceNotification update(ResourceNotification notification) {
        return null;
    }

    @Override
    public ResourceNotification delete(ResourceNotification notification) {
        return null;
    }

}

package com.sequenceiq.freeipa.service;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

@Component
public class MinimalPersisterService implements Persister<ResourceNotification> {

    @Override
    public ResourceNotification persist(ResourceNotification data) {
        return new ResourceNotification(data.getCloudResource(), data.getCloudContext(), data.getType());
    }

    @Override
    public ResourceNotification update(ResourceNotification data) {
        return new ResourceNotification(data.getCloudResource(), data.getCloudContext(), data.getType());
    }

    @Override
    public ResourceNotification delete(ResourceNotification data) {
        return new ResourceNotification(data.getCloudResource(), data.getCloudContext(), data.getType());
    }
}

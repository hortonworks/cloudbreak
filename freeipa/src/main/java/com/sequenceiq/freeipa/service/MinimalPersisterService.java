package com.sequenceiq.freeipa.service;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;

@Component
public class MinimalPersisterService implements Persister<ResourceNotification> {

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Override
    public ResourceNotification persist(ResourceNotification data) {
        return new ResourceNotification(data.getCloudResource(), data.getCloudContext(), data.getType());
    }

    @Override
    public ResourceNotification update(ResourceNotification data) {
        return new ResourceNotification(data.getCloudResource(), data.getCloudContext(), data.getType());
    }

    @Override
    public ResourceNotification retrieve(ResourceNotification data) {
        return null;
    }

    @Override
    public ResourceNotification delete(ResourceNotification data) {
        return new ResourceNotification(data.getCloudResource(), data.getCloudContext(), data.getType());
    }
}

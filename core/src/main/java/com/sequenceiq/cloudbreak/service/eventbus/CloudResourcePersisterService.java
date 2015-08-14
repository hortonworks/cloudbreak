package com.sequenceiq.cloudbreak.service.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceAllocationNotification;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class CloudResourcePersisterService extends AbstractCloudPersisterService<ResourceAllocationNotification> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Override
    public ResourceAllocationNotification persist(ResourceAllocationNotification data) {
        LOGGER.debug("Persisting resource allocation notification data: {}", data);
        StackRepository stackRepo = (StackRepository) getRepositoryForEntity(new Stack());
        Resource resource = getConversionService().convert(data.getCloudResource(), Resource.class);
        resource.setStack(stackRepo.findById(data.getStackId()));
        resource = (Resource) getRepositoryForEntity(resource).save(resource);
        CloudResource cloudResource = getConversionService().convert(resource, CloudResource.class);
        // TODO this conversion might not be necessary
        data = new ResourceAllocationNotification(cloudResource, data.getStackId(), data.getPromise());
        return data;
    }

    @Override
    public ResourceAllocationNotification retrieve(ResourceAllocationNotification data) {
        return null;
    }
}

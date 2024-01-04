package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceRetrievalNotification;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class ResourceRetrievalHandler implements Consumer<Event<ResourceRetrievalNotification>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRetrievalHandler.class);

    @Inject
    private ResourceRetriever cloudResourceRetrieverService;

    @Override
    public void accept(Event<ResourceRetrievalNotification> event) {
        LOGGER.debug("Resource retrieval notification event received: {}", event);
        ResourceRetrievalNotification data = event.getData();
        List<CloudResource> resources = retrieveResource(data);
        data.getPromise().onNext(resources);
    }

    private List<CloudResource> retrieveResource(ResourceRetrievalNotification data) {
        List<CloudResource> result = new ArrayList<>();
        if (data.getStackId() != null && CollectionUtils.isNotEmpty(data.getResourceReferences())) {
            result = cloudResourceRetrieverService.findByResourceReferencesAndStatusAndTypeAndStack(data.getResourceReferences(),
                    data.getStatus(), data.getResourceType(), data.getStackId());
        } else if (data.getStackId() != null && CollectionUtils.isEmpty(data.getResourceReferences())) {
            Optional<CloudResource> resource = cloudResourceRetrieverService.findByStatusAndTypeAndStack(data.getStatus(), data.getResourceType(),
                    data.getStackId());
            if (resource.isPresent()) {
                result.add(resource.get());
            }
        } else {
            result = cloudResourceRetrieverService.findByResourceReferencesAndStatusAndType(data.getResourceReferences(), data.getStatus(),
                    data.getResourceType());
        }
        return result;
    }
}

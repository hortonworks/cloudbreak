package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CollectMetadataHandler implements CloudPlatformEventHandler<CollectMetadataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectMetadataHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<CollectMetadataRequest> type() {
        return CollectMetadataRequest.class;
    }

    @Override
    public void accept(Event<CollectMetadataRequest> collectMetadataRequestEvent) {
        LOGGER.debug("Received event: {}", collectMetadataRequestEvent);
        CollectMetadataRequest request = collectMetadataRequestEvent.getData();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());

            List<CloudVmMetaDataStatus> instanceStatuses = connector.metadata()
                    .collect(ac, request.getCloudResource(), request.getVms(), request.getKnownVms());
            CollectMetadataResult collectMetadataResult = new CollectMetadataResult(request, instanceStatuses);
            request.getResult().onNext(collectMetadataResult);
            eventBus.notify(collectMetadataResult.selector(), new Event<>(collectMetadataRequestEvent.getHeaders(), collectMetadataResult));
            LOGGER.debug("Metadata collection successfully finished");
        } catch (RuntimeException e) {
            CollectMetadataResult failure = new CollectMetadataResult(e, request);
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(collectMetadataRequestEvent.getHeaders(), failure));
        }
    }
}

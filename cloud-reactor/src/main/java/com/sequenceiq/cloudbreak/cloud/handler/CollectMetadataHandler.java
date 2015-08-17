package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

import reactor.bus.Event;

@Component
public class CollectMetadataHandler implements CloudPlatformEventHandler<CollectMetadataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectMetadataHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<CollectMetadataRequest> type() {
        return CollectMetadataRequest.class;
    }

    @Override
    public void accept(Event<CollectMetadataRequest> launchStackRequestEvent) {
        LOGGER.info("Received event: {}", launchStackRequestEvent);
        CollectMetadataRequest<CollectMetadataResult> request = launchStackRequestEvent.getData();
        try {
            String platform = request.getCloudContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext ac = connector.authentication().authenticate(request.getCloudContext(), request.getCloudCredential());
            List<CloudVmInstanceStatus> instanceStatuses = connector.instances().metadata().collect(ac, request.getCloudResource(), request.getVms());
            CollectMetadataResult collectMetadataResult = new CollectMetadataResult(request.getCloudContext(), instanceStatuses);
            request.getResult().onNext(collectMetadataResult);
            LOGGER.info("Metadata collection successfully finished");
        } catch (Exception e) {
            request.getResult().onNext(new CollectMetadataResult(request.getCloudContext(), e));
        }
    }

}

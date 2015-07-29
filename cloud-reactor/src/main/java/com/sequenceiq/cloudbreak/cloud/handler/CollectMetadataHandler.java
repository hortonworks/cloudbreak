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
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

import reactor.bus.Event;

@Component
public class CollectMetadataHandler implements CloudPlatformEventHandler<CollectMetadataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectMetadataHandler.class);

    private static final int INTERVAL = 5;
    private static final int MAX_ATTEMPT = 100;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private SyncPollingScheduler<LaunchStackResult> syncPollingScheduler;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Override
    public Class<CollectMetadataRequest> type() {
        return CollectMetadataRequest.class;
    }

    @Override
    public void accept(Event<CollectMetadataRequest> launchStackRequestEvent) {
        LOGGER.info("Received event: {}", launchStackRequestEvent);
        CollectMetadataRequest collectMetadataRequest = launchStackRequestEvent.getData();
        try {
            String platform = collectMetadataRequest.getCloudContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext ac = connector.authenticate(collectMetadataRequest.getCloudContext(), collectMetadataRequest.getCloudCredential());

            List<CloudVmInstanceStatus> instanceStatuses = connector.instances().collectMetadata(ac, collectMetadataRequest.getCloudResource(),
                    collectMetadataRequest.getVms());


            CollectMetadataResult collectMetadataResult = new CollectMetadataResult(collectMetadataRequest.getCloudContext(), instanceStatuses);


            collectMetadataRequest.getResult().onNext(collectMetadataResult);

        } catch (Exception e) {
            LOGGER.error("Failed to handle LaunchStackRequest. Error: ", e);
            // TODO send error reason
            collectMetadataRequest.getResult().onNext(new CollectMetadataResult(collectMetadataRequest.getCloudContext(), null));
        }
        LOGGER.info("LaunchStackHandler finished");
    }


}

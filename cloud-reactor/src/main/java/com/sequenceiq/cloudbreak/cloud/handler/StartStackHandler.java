package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

import reactor.bus.Event;

@Component
public class StartStackHandler implements CloudPlatformEventHandler<StartInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartStackHandler.class);
    private static final int INTERVAL = 5;
    private static final int MAX_ATTEMPT = 100;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<InstancesStatusResult> syncPollingScheduler;

    @Override
    public Class<StartInstancesRequest> type() {
        return StartInstancesRequest.class;
    }

    @Override
    public void accept(Event<StartInstancesRequest> event) {
        LOGGER.info("Received event: {}", event);
        StartInstancesRequest request = event.getData();
        try {
            CloudContext cloudContext = request.getCloudContext();
            String platform = cloudContext.getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext authenticatedContext = connector.authenticate(cloudContext, request.getCloudCredential());
            List<CloudInstance> instances = request.getCloudInstances();
            List<CloudVmInstanceStatus> instanceStatuses = connector.instances().start(authenticatedContext, instances);

            PollTask<InstancesStatusResult> task = statusCheckFactory.newPollInstanceStateTask(authenticatedContext, instances);
            InstancesStatusResult statusResult = new InstancesStatusResult(cloudContext, instanceStatuses);
            if (!task.completed(statusResult)) {
                statusResult = syncPollingScheduler.schedule(task, INTERVAL, MAX_ATTEMPT);
            }
            //TODO check if state is FAILED
            request.getResult().onNext(new StartInstancesResult(cloudContext, "Stack successfully started", statusResult));
        } catch (Exception e) {
            LOGGER.error("Failed to handle StopStackRequest.", e);
            request.getResult().onNext(new SetupResult(e, request));
        }
        LOGGER.info("StopStackHandler finished");
    }


}

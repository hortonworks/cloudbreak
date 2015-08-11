package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

import reactor.bus.Event;

@Component
public class StopStackHandler implements CloudPlatformEventHandler<StopInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStackHandler.class);
    private static final int INTERVAL = 5;
    private static final int MAX_ATTEMPT = 100;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<InstancesStatusResult> syncPollingScheduler;

    @Override
    public Class<StopInstancesRequest> type() {
        return StopInstancesRequest.class;
    }

    @Override
    public void accept(Event<StopInstancesRequest> event) {
        LOGGER.info("Received event: {}", event);
        StopInstancesRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            String platform = cloudContext.getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            List<CloudInstance> instances = request.getCloudInstances();
            AuthenticatedContext authenticatedContext = connector.authenticate(cloudContext, request.getCloudCredential());
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = connector.instances().stop(authenticatedContext, instances);

            PollTask<InstancesStatusResult> task = statusCheckFactory.newPollInstanceStateTask(authenticatedContext, instances);
            InstancesStatusResult statusResult = new InstancesStatusResult(cloudContext, cloudVmInstanceStatuses);
            if (!task.completed(statusResult)) {
                statusResult = syncPollingScheduler.schedule(task, INTERVAL, MAX_ATTEMPT);
            }
            request.getResult().onNext(new StopInstancesResult(cloudContext, statusResult));
        } catch (Exception e) {
            request.getResult().onNext(new StopInstancesResult(cloudContext, e));
        }
    }


}

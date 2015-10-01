package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

import reactor.bus.Event;

@Component
public class StartStackHandler implements CloudPlatformEventHandler<StartInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartStackHandler.class);

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
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext authenticatedContext = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudInstance> instances = request.getCloudInstances();
            List<CloudVmInstanceStatus> instanceStatuses = connector.instances().start(authenticatedContext, request.getCloudStack(), request.getResources(),
                    instances);
            PollTask<InstancesStatusResult> task = statusCheckFactory.newPollInstanceStateTask(authenticatedContext, request.getCloudStack(), instances,
                    Sets.newHashSet(InstanceStatus.STARTED, InstanceStatus.FAILED));
            InstancesStatusResult statusResult = new InstancesStatusResult(cloudContext, instanceStatuses);
            if (!task.completed(statusResult)) {
                statusResult = syncPollingScheduler.schedule(task);
            }
            request.getResult().onNext(new StartInstancesResult(cloudContext, statusResult));
        } catch (Exception e) {
            request.getResult().onNext(new StartInstancesResult(cloudContext, e));
        }
    }

}

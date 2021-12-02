package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesResult;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StopStartDownscaleStopInstancesHandler implements CloudPlatformEventHandler<StopStartDownscaleStopInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartDownscaleStopInstancesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<StopStartDownscaleStopInstancesRequest> type() {
        return StopStartDownscaleStopInstancesRequest.class;
    }

    @Override
    public void accept(Event<StopStartDownscaleStopInstancesRequest> event) {
        StopStartDownscaleStopInstancesRequest request = event.getData();
        LOGGER.info("StopStartDownscaleStopInstancesHandler: {}", request);


        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request, cloudContext, connector);

            List<CloudInstance> cloudInstancesToStop = request.getCloudInstancesToStop();
            List<CloudVmInstanceStatus> cloudVmInstanceStatusList = Collections.emptyList();
            if (cloudInstancesToStop.size() > 0) {
                LOGGER.info("ZZZ: Attempting to stop instances");
                cloudVmInstanceStatusList = connector.instances().stop(ac, null, cloudInstancesToStop);
                LOGGER.info("ZZZ: CloudVMInstanceStatusesPostStop={}", cloudVmInstanceStatusList);
            } else {
                LOGGER.info("ZZZ: Did not find any instances to stop");
            }

            // TODO CB-14929: If we fail to STOP all instances - one potential path for error handling would be to allow a subsequent upscale operation
            //  to consider nodes which are in DECOMMISSIONED state, but RUNNING - as candidates for UPSCALE.
            // TODO CB-14929: If we fail to STOP all instances - make sure the information is at least available on the Activity log
            //  (ideally in some easily accessible alerting mechanism / cluster needs attention section)
            // TODO CB-14929: Potentially introduce a new node-state for such instances

            StopStartDownscaleStopInstancesResult result = new StopStartDownscaleStopInstancesResult(request.getResourceId(),
                    cloudInstancesToStop, cloudVmInstanceStatusList);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            // TODO CB-14929: How should exceptions from handlers be handled?
            throw e;
        }
    }

    private AuthenticatedContext getAuthenticatedContext(StopStartDownscaleStopInstancesRequest<StopStartDownscaleStopInstancesResult> request,
            CloudContext cloudContext, CloudConnector<?> connector) {
        return connector.authentication().authenticate(cloudContext, request.getCloudCredential());
    }
}

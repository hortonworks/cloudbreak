package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StopStartDownscaleStopInstancesHandler implements CloudPlatformEventHandler<StopStartDownscaleStopInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartDownscaleStopInstancesHandler.class);

    private static final Long STOP_POLL_TIMEBOUND_MS = 600_000L;

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
        LOGGER.info("StopStartDownscaleStopInstancesHandler: {}", event.getData().getResourceId());

        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request, cloudContext, connector);

            List<CloudInstance> cloudInstancesToStop = request.getCloudInstancesToStop();
            List<CloudVmInstanceStatus> cloudVmInstanceStatusList = Collections.emptyList();
            if (cloudInstancesToStop.size() > 0) {
                LOGGER.info("Attempting to stop instances with a timebound of {}ms. count={}, instances=[{}]",
                        STOP_POLL_TIMEBOUND_MS, cloudInstancesToStop.size(), cloudInstancesToStop.stream().map(CloudInstance::getInstanceId)
                                .collect(Collectors.toList()));
                // TODO CB-15132: CB-15342 In case of a failure in this step, the nodes stay in a DECOMMISSIONED state,
                //  even if the services are started via CM. The StackStatusChckerJob appears to ignore a bunch of CM states.
                // TODO CB-15132: What happens if the cloud provider does not know about an instance for which a STOP was requested. How does the API
                //  behave.
                cloudVmInstanceStatusList = connector.instances().stopWithLimitedRetry(ac, null, cloudInstancesToStop, STOP_POLL_TIMEBOUND_MS);
            } else {
                LOGGER.info("No cloud VM instances to stop. Succeeding flow step with no action taken");
            }
            LOGGER.trace("CloudVMInstanceStatusesPostStop={}", cloudVmInstanceStatusList);

            // TODO CB-15132: If we fail to STOP all instances - one potential path for error handling would be to allow a subsequent upscale operation
            //  to consider nodes which are in DECOMMISSIONED state, but RUNNING - as candidates for UPSCALE.
            StopStartDownscaleStopInstancesResult result = new StopStartDownscaleStopInstancesResult(request.getResourceId(),
                    request, cloudVmInstanceStatusList);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            // TODO CB-15132: Try propagating specific information in the error, so that a later step can potentially attempt
            //  to recover from this, or proceed with a reduced set of nodes.
            String message = "Failed while attempting to stop some instances";
            LOGGER.error(message, e);
            StopStartDownscaleStopInstancesResult result = new StopStartDownscaleStopInstancesResult(message, e, request.getResourceId(), request);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        }
    }

    private AuthenticatedContext getAuthenticatedContext(StopStartDownscaleStopInstancesRequest request,
            CloudContext cloudContext, CloudConnector<?> connector) {
        return connector.authentication().authenticate(cloudContext, request.getCloudCredential());
    }
}

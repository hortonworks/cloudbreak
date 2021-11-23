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
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesResult;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StopStartUpscaleStartInstancesHandler implements CloudPlatformEventHandler<StopStartUpscaleStartInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartUpscaleStartInstancesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<StopStartUpscaleStartInstancesRequest> type() {
        return StopStartUpscaleStartInstancesRequest.class;
    }

    @Override
    public void accept(Event<StopStartUpscaleStartInstancesRequest> event) {
        StopStartUpscaleStartInstancesRequest request = event.getData();
        LOGGER.info("StopStartUpscaleStartInstancesHandler: {}", event.getData().getResourceId());

        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request, cloudContext, connector);

            List<CloudInstance> stoppedInstancesInHg = request.getStoppedCloudInstancesInHg();
            int numInstancesToStart = request.getNumInstancesToStart() < stoppedInstancesInHg.size() ? request.getNumInstancesToStart() : stoppedInstancesInHg.size();
            LOGGER.info("ZZZ: Requested instances to start: {}, actual instances being started: {}", request.getNumInstancesToStart(), numInstancesToStart);

            // TODO CB-14929: This should ideally be randomized a bit, so that different isntances are started/stopped each time. That said, there is value in
            //  a reliable pattern. i.e. something along the lines of sort by hostname within this list.

            List<CloudInstance> instancesToStart = request.getStoppedCloudInstancesInHg().subList(0, numInstancesToStart);
            // TODO CB-14929: Additional validation here to check if these instances are actually in STOPPED state (as against the current list which is an indication of what the Cloudbreak Database thinks are STOPPED instances)

            LOGGER.info("ZZZ: Instance identified as start candidates: count={}, instances={}", instancesToStart.size(), instancesToStart);
            if (instancesToStart.size() < request.getNumInstancesToStart()) {
                LOGGER.info("ZZZ: There are fewer instances available to start as compared to the request. Requested: {}, Available: {}", request.getNumInstancesToStart(), instancesToStart.size());
            }

            List<CloudVmInstanceStatus> instanceStatuses = Collections.emptyList();
            if (instancesToStart.size() > 0) {
                LOGGER.info("ZZZ: Attempting to start instances");
                instanceStatuses = connector.instances().start(ac, null, instancesToStart);
                LOGGER.info("ZZZ: Instance start attempt complete");
            } else {
                LOGGER.info("ZZZ: Did not find any instances to start");
            }

            // TODO CB-14929: If we are not able to start adequate resources, consider going to the cloud-provider to check instance state, in case CB DB entries are out of sync.
            // TODO CB-14929: Timebound this operation to X minutes. Start whatever is possible within this duration.
            // TODO CB-14929: start API call in the library may need to be modified to handle errors differently, given the time duration check we want.
            // TODO CB-14929: Optionally factor in nodes which are running but have CM services in STOPPED/DECOMMISSIONED state
            // TODO CB-14929: Error Handling: If instances fail to START - do we need to actively try STOPPING them? In case of failure, make sure the list is propagated up for activity/needs-attention.

            // TODO CB-14929: Tempoarilty assuming that all isntances started successfully. Not bothering to map back from the results vs the originally computed list.
            //  so instancesToStart is the metaData that can be sent to the next stage. Normally, this would need to be filtered and re-built for the next step.
            StopStartUpscaleStartInstancesResult result = new StopStartUpscaleStartInstancesResult(request.getResourceId(), instanceStatuses);

            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            // TODO CB-14929: Handle exceptions in a useful way. Ideally need to leave the cluster in a usable state after failure of the flow.
            throw e;
        }
    }

    private AuthenticatedContext getAuthenticatedContext(StopStartUpscaleStartInstancesRequest<StopStartUpscaleStartInstancesResult> request, CloudContext cloudContext,
            CloudConnector<?> connector) {
        return connector.authentication().authenticate(cloudContext, request.getCloudCredential());
    }
}

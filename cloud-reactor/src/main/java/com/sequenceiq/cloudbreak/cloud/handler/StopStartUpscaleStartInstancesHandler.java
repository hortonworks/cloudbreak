package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StopStartUpscaleStartInstancesHandler implements CloudPlatformEventHandler<StopStartUpscaleStartInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartUpscaleStartInstancesHandler.class);

    private static final Long START_POLL_TIMEBOUND_MS = 300_000L;

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

        int numInstancesToStart = request.getNumInstancesToStart() - request.getStartedInstancesWithServicesNotRunning().size();
        if (numInstancesToStart <= 0) {
            // TODO CB-15132: This is currently not exercised, since the Flow itself does not send any good information about running instances.
            LOGGER.debug("No nodes to start. Start requested for numInstances={}, Running CM instances with services not running={}",
                    request.getNumInstancesToStart(), request.getStartedInstancesWithServicesNotRunning().size());
            List<CloudInstance> startedInstancesWithServicesNotRunning = request.getStartedInstancesWithServicesNotRunning();
            List<CloudVmInstanceStatus> vmInstanceStatuses = startedInstancesWithServicesNotRunning.stream()
                    .map(i -> new CloudVmInstanceStatus(i, InstanceStatus.STARTED)).collect(Collectors.toUnmodifiableList());
            StopStartUpscaleStartInstancesResult result = new StopStartUpscaleStartInstancesResult(
                    request.getResourceId(), request, vmInstanceStatuses);
            notify(result, event);
            return;
        }

        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request, cloudContext, connector);

            List<CloudInstance> stoppedInstancesInCbHg = request.getStoppedCloudInstancesInHg();
            List<CloudVmInstanceStatus> stoppedInstancesOnCloudProvider = Collections.emptyList();

            List<CloudInstance> instancesToStart = Collections.emptyList();

            if (numInstancesToStart > stoppedInstancesInCbHg.size()) {
                // See if we can find additional instances on the cloud-provider which can potentially be STARTED.
                stoppedInstancesOnCloudProvider = collectStoppedInstancesFromCloudProvider(connector, ac, request.getAllInstancesInHg());
                instancesToStart = getInstancesToStart(stoppedInstancesInCbHg, stoppedInstancesOnCloudProvider, request.getHostGroupName(), numInstancesToStart);
            } else {
                // TODO CB-15132: Potentially validate against the cloud-provider to make sure these instances are actually in STOPPED state.
                // Try starting the required instances based on CB state.
                instancesToStart = stoppedInstancesInCbHg.subList(0, numInstancesToStart);
                // TODO CB-15132: If there's a failure on this particular operation - go back to the cloud provider to try finding additional STOPPED instances,
                // and try STARTING these as well.
            }
            LOGGER.info("Requested instances to start={}, actual instances being started={}, numInstancesWithServicesNotRunning={}",
                    request.getNumInstancesToStart(), instancesToStart.size(), request.getStartedInstancesWithServicesNotRunning().size());

            LOGGER.debug("Attempting to start instances. count={}, instanceIds={}",
                    instancesToStart.size(), instancesToStart.stream().map(i -> i.getInstanceId()).collect(Collectors.toList()));

            List<CloudVmInstanceStatus> instanceStatuses = Collections.emptyList();

            if (instancesToStart.size() > 0) {
                instanceStatuses = startInstances(connector, ac, instancesToStart);
                LOGGER.info("Started instances. Result: {}", instanceStatuses);
            } else {
                LOGGER.info("No instances to start");
            }

            StopStartUpscaleStartInstancesResult result = new StopStartUpscaleStartInstancesResult(request.getResourceId(), request, instanceStatuses);

            notify(result, event);
        } catch (Exception e) {
            // TODO CB-15132: Try propagating specific information in the error, so that a later step can potentially attempt
            //  to recover from this, or proceed with a reduced set of nodes.
            String message = "Failed while attempting to start instances";
            LOGGER.error(message);
            StopStartUpscaleStartInstancesResult result = new StopStartUpscaleStartInstancesResult(message, e, request.getResourceId(), request);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        }
    }

    private List<CloudVmInstanceStatus> startInstances(
            CloudConnector<?> connector, AuthenticatedContext ac, List<CloudInstance> instancesToStart) {
        // Note: This timebound can apply thrice along with some delays, given how retries on this API are currently configured for AWS.
        //  The rest of the cloud providers is an UNKNOWN.
        return connector.instances().startWithLimitedRetry(ac, null, instancesToStart, START_POLL_TIMEBOUND_MS);
    }

    private List<CloudVmInstanceStatus> collectStoppedInstancesFromCloudProvider(
            CloudConnector<?> connector, AuthenticatedContext ac, List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> vmInstanceStatusAllInstances = connector.instances().checkWithoutRetry(ac, cloudInstances);
        return vmInstanceStatusAllInstances.stream().filter(vm -> vm.getStatus() == InstanceStatus.STOPPED).collect(Collectors.toList());
    }

    private List<CloudInstance> getInstancesToStart(List<CloudInstance> stoppedInstancesInCb,
            List<CloudVmInstanceStatus> stoppedInstancesOnCloudProvider, String hostGroupName, int numInstancesToStart) {
        // Instances considered to be STOPPED on the cloud-provider get precendece,
        // unless there was an error retrieving instance metadata from the cloud provider - in which case
        // we fail the flow. The assumption is that the next attempt will try the same again,
        // and should succeed.
        if (stoppedInstancesInCb.size() != stoppedInstancesOnCloudProvider.size()) {
            logInfoAboutCbCloudProviderStoppedInstanceDiscrepency(stoppedInstancesInCb, stoppedInstancesOnCloudProvider, hostGroupName);
        }
        return stoppedInstancesOnCloudProvider.stream().limit(numInstancesToStart).map(vmi -> vmi.getCloudInstance()).collect(Collectors.toList());
    }

    private void logInfoAboutCbCloudProviderStoppedInstanceDiscrepency(List<CloudInstance> stoppedInstancesInCb,
            List<CloudVmInstanceStatus> stoppedInstancesOnCloudProvider, String hostGroupName) {
        Set<String> stoppedInstancesInCbSet = stoppedInstancesInCb.stream().map(i -> i.getInstanceId()).collect(Collectors.toUnmodifiableSet());
        Set<String> stoppedInstancesOnCloudProviderSet = stoppedInstancesOnCloudProvider.stream()
                .map(i -> i.getCloudInstance().getInstanceId()).collect(Collectors.toUnmodifiableSet());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("For hostGroup: %s, NumberOfStoppedInstancesKnownToCB: %d, numberOfStoppedInstancesOnCloudProvider: %d.",
                hostGroupName, stoppedInstancesInCb.size(), stoppedInstancesOnCloudProvider.size()));
        List<String> missingStoppedInstancesCb = stoppedInstancesOnCloudProvider.stream()
                .filter(vmi -> !stoppedInstancesInCbSet.contains(vmi.getCloudInstance().getInstanceId()))
                .map(vmi -> vmi.getCloudInstance().getInstanceId())
                .collect(Collectors.toList());
        List<String> missingStoppedInstancesCloudProvider = stoppedInstancesInCb.stream()
                .filter(i -> !stoppedInstancesOnCloudProviderSet.contains(i.getInstanceId()))
                .map(i -> i.getInstanceId())
                .collect(Collectors.toList());


        if (!missingStoppedInstancesCb.isEmpty()) {
            sb.append(String.format(" Found instances stopped on cloud provider but not in CB. count=%d, instances=[%s].",
                    missingStoppedInstancesCb.size(), missingStoppedInstancesCb));
        }
        if (!missingStoppedInstancesCloudProvider.isEmpty()) {
            sb.append(String.format(" Found instances stopped in CB but not in stopped state on cloud provider. count=%d, instances=[%s].",
                    missingStoppedInstancesCloudProvider.size(), missingStoppedInstancesCloudProvider));
        }
        LOGGER.info(sb.toString());
    }

    protected void notify(StopStartUpscaleStartInstancesResult result, Event<StopStartUpscaleStartInstancesRequest> event) {
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private AuthenticatedContext getAuthenticatedContext(StopStartUpscaleStartInstancesRequest request,
            CloudContext cloudContext, CloudConnector<?> connector) {
        return connector.authentication().authenticate(cloudContext, request.getCloudCredential());
    }
}

package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;

@Component
public class StopStartUpscaleStartInstancesHandler implements CloudPlatformEventHandler<StopStartUpscaleStartInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartUpscaleStartInstancesHandler.class);

    private static final Long START_POLL_TIMEBOUND_MS = 600_000L;

    private static final Long INSTANCE_SIZE_DECREMENT_FACTOR = 2L;

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
            LOGGER.debug("No nodes to start. Start requested for numInstances={}, Running CM instances with services not running={}",
                    request.getNumInstancesToStart(), request.getStartedInstancesWithServicesNotRunning().size());
            StopStartUpscaleStartInstancesResult result = new StopStartUpscaleStartInstancesResult(
                    request.getResourceId(), request, Collections.emptyList());
            notify(result, event);
            return;
        }

        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request, cloudContext, connector);

            List<CloudInstance> stoppedInstancesInCbHg = request.getStoppedCloudInstancesInHg();
            List<CloudVmInstanceStatus> stoppedInstancesOnCloudProvider = Collections.emptyList();

            List<CloudInstance> instancesToStart = Collections.emptyList();

            if (numInstancesToStart > stoppedInstancesInCbHg.size()) {
                // See if we can find additional instances on the cloud-provider which can potentially be STARTED.
                stoppedInstancesOnCloudProvider = collectStoppedInstancesFromCloudProvider(connector, ac, request.getAllInstancesInHg());
                instancesToStart = getInstancesToStart(stoppedInstancesInCbHg, stoppedInstancesOnCloudProvider, request.getHostGroupName(), numInstancesToStart);
            } else {
                // Filter based on CB state, but confirm against the cloud provider. It is OK to start fewer instances than requested.
                // TODO CB-15132: We could go back to the cloud provider and try finding additional stopped instances.
                stoppedInstancesOnCloudProvider = collectStoppedInstancesFromCloudProvider(connector, ac, stoppedInstancesInCbHg);
                instancesToStart = stoppedInstancesOnCloudProvider
                        .stream()
                        .map(CloudVmInstanceStatus::getCloudInstance)
                        .limit(numInstancesToStart)
                        .toList();
            }

            LOGGER.info("Requested instances to start={}, actual instances being started={}, numInstancesWithServicesNotRunning={}" +
                    ", numStoppedInstanceCountInCbHg={}, numStoppedInstancesOnCloudProvider={}",
                    request.getNumInstancesToStart(), instancesToStart.size(), request.getStartedInstancesWithServicesNotRunning().size(),
                    stoppedInstancesInCbHg.size(), stoppedInstancesOnCloudProvider.size());

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
            notify(result, event);
        }
    }

    private List<CloudVmInstanceStatus> startInstances(
            CloudConnector connector, AuthenticatedContext ac, List<CloudInstance> instancesToStart) {
        // Note: This timebound can apply thrice along with some delays, given how retries on this API are currently configured for AWS.
        //  The rest of the cloud providers is an UNKNOWN.
        try {
            return connector.instances().startWithLimitedRetry(ac, null, instancesToStart, START_POLL_TIMEBOUND_MS);
        } catch (PollerStoppedException p) {
            // We exceeded the time-bound on the polling. Try getting the status for all the instances that we attempted to start.
            LOGGER.warn("Timed out while attempting to start instances. Attempting to get states for attempted nodes", p);
            return getInstanceStatusOnStartError(connector, ac, instancesToStart, p);
        } catch (InsufficientCapacityException ice) {
            LOGGER.error("Insufficient capacity on the cloud provider while attempting to start instances. Trying to fetch statuses of nodes", ice);
            return startInstancesRetryable(connector, ac, instancesToStart);
        } catch (Exception e) {
            // Some other error while attempting to startInstances. Try getting the status for all the instances that we attempted to start.
            LOGGER.warn("Exception while attempting to start instances. Attempting to get states for attempted nodes", e);
            return getInstanceStatusOnStartError(connector, ac, instancesToStart, e);
        }
    }

    @Retryable(value = InsufficientCapacityException.class, maxAttempts = 3, backoff = @Backoff(delay = 5000, multiplier = 0.5))
    private List<CloudVmInstanceStatus> startInstancesRetryable(CloudConnector connector, AuthenticatedContext ac, List<CloudInstance> instancesToStart) {
        // TODO We need to create a sublist of instances to start. How do we choose this subset of nodes?
        return connector.instances().startWithLimitedRetry(ac, null, instancesToStart, START_POLL_TIMEBOUND_MS);
    }

    private List<CloudVmInstanceStatus> getInstanceStatusOnStartError(CloudConnector connector, AuthenticatedContext ac,
            List<CloudInstance> instancesToStart, Exception originalException) {
        try {
            return connector.instances().checkWithoutRetry(ac, instancesToStart);
        } catch (Exception e) {
            LOGGER.warn("Error while trying to get instance status after start failure. Propagating original error from the start attempt", e);
            // The PollerStoppedException is too verbose for the console.
            String message = "Error while attempting to start instances";
            if (originalException instanceof PollerStoppedException) {
                message = "Timed out while waiting for instances to start";
            } else if (originalException instanceof InsufficientCapacityException) {
                message = "Insufficient instance capacity on the cloud provider";
            }
            throw new CloudbreakRuntimeException(message, originalException);
        }
    }

    private List<CloudVmInstanceStatus> collectStoppedInstancesFromCloudProvider(
            CloudConnector connector, AuthenticatedContext ac, List<CloudInstance> cloudInstances) {
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
            CloudContext cloudContext, CloudConnector connector) {
        return connector.authentication().authenticate(cloudContext, request.getCloudCredential());
    }
}

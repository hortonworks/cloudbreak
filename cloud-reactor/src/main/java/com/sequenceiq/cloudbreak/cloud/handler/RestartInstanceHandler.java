package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.RestartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.RestartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;

@Component
public class RestartInstanceHandler implements CloudPlatformEventHandler<RestartInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartInstanceHandler.class);

    private static final List<InstanceStatus> EXCLUDED_STATUSES = List.of(
            InstanceStatus.STOPPED,
            InstanceStatus.ZOMBIE,
            InstanceStatus.TERMINATED,
            InstanceStatus.TERMINATED_BY_PROVIDER,
            InstanceStatus.DELETE_REQUESTED
    );

    private static final Long RESTART_POLL_TIMEBOUND_MS = 1_200_000L;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<RestartInstancesRequest> type() {
        return RestartInstancesRequest.class;
    }

    @Override
    public void accept(Event<RestartInstancesRequest> event) {
        LOGGER.info("RestartInstancesHandler: {}", event.getData().getResourceId());
        RestartInstancesRequest<RestartInstancesResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext authenticatedContext = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = restartInstances(connector, authenticatedContext,
                    request.getCloudInstances(), request.getCloudResources());
            List<CloudInstance> succesfullyRestartedCloudInstance = cloudVmInstanceStatuses.stream().map(CloudVmInstanceStatus::getCloudInstance)
                    .collect(Collectors.toList());
            List<CloudInstance> failedToRestartCloudInstance = request.getCloudInstances().stream().filter(i -> !succesfullyRestartedCloudInstance.contains(i))
                    .collect(Collectors.toList());

            LOGGER.info("Restarted instances. Result: Successfully Restarted:[{}]. Failed to Restart:[{}]", succesfullyRestartedCloudInstance,
                    failedToRestartCloudInstance);
            InstancesStatusResult statusResult = new InstancesStatusResult(cloudContext, cloudVmInstanceStatuses);
            RestartInstancesResult result = new RestartInstancesResult(request.getResourceId(), statusResult, getInstances(request));
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            String message = "Failed while attempting to restart instances";
            LOGGER.error(e.getMessage());
            RestartInstancesResult failure = new RestartInstancesResult(message, e, request.getResourceId(),
                    getInstances(request));
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }

    private List<CloudVmInstanceStatus> restartInstances(CloudConnector connector, AuthenticatedContext ac,
            List<CloudInstance> instancesToRestart, List<CloudResource> cloudResources) {
        try {
            List<CloudVmInstanceStatus> restartedCloudInstance = connector.instances()
                    .restartWithLimitedRetry(ac,  cloudResources, instancesToRestart, RESTART_POLL_TIMEBOUND_MS, EXCLUDED_STATUSES);
            return restartedCloudInstance.stream().filter(i -> i.getStatus().equals(InstanceStatus.STARTED)).collect(Collectors.toList());
        } catch (PollerStoppedException p) {
            LOGGER.warn("Timed out while attempting to restart instances. Attempting to get states for attempted nodes", p);
            return getInstanceStatusOnStartError(connector, ac, instancesToRestart, p);
        } catch (Exception e) {
            LOGGER.warn("Exception while attempting to restart instances. Attempting to get states for attempted nodes", e);
            return getInstanceStatusOnStartError(connector, ac, instancesToRestart, e);
        }
    }

    private List<CloudVmInstanceStatus> getInstanceStatusOnStartError(CloudConnector connector, AuthenticatedContext ac,
            List<CloudInstance> instancesToRestart, Exception originalException) {
        try {
            List<CloudVmInstanceStatus> instanceStatuses = connector.instances().checkWithoutRetry(ac, instancesToRestart);
            return instanceStatuses.stream().filter(i -> i.getStatus().equals(InstanceStatus.STARTED)).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.warn("Error while trying to get instance status after restart failure. Propagating original error from the restart attempt", e);
            String message = "Error while attempting to restart instances";
            if (originalException instanceof PollerStoppedException) {
                message = "Timed out while waiting for instances to restart";
            }
            throw new CloudbreakRuntimeException(message, originalException);
        }
    }

    private List<String> getInstances(RestartInstancesRequest<RestartInstancesResult> request) {
        return request.getCloudInstances().stream().map(CloudInstance::getInstanceId).collect(Collectors.toList());
    }
}

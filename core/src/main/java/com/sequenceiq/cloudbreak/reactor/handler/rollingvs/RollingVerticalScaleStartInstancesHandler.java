package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

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
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesResult;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RollingVerticalScaleStartInstancesHandler extends ExceptionCatcherEventHandler<RollingVerticalScaleStartInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleStartInstancesHandler.class);

    private static final Long RESTART_POLL_TIMEBOUND_MS = 1_200_000L;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollingVerticalScaleStartInstancesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollingVerticalScaleStartInstancesRequest> event) {
        LOGGER.error("Unexpected exception occurred in RollingVerticalScaleStartInstancesHandler for resourceId: {}", resourceId, e);
        return new StackFailureEvent(RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FAILURE_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollingVerticalScaleStartInstancesRequest> event) {
        LOGGER.info("RollingVerticalScaleStartInstancesHandler: {}", event.getData().getResourceId());
        RollingVerticalScaleStartInstancesRequest request = event.getData();
        RollingVerticalScaleResult rollingVerticalScaleResult = request.getRollingVerticalScaleResult();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext authenticatedContext = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = startInstances(connector, authenticatedContext,
                    request.getCloudInstances(), request.getCloudResources());
            updateRollingVerticalScaleResult(rollingVerticalScaleResult, cloudVmInstanceStatuses);

            List<String> successfullyStartedCloudInstance = cloudVmInstanceStatuses.stream()
                    .map(CloudVmInstanceStatus::getCloudInstance)
                    .map(CloudInstance::getInstanceId)
                    .collect(Collectors.toList());
            List<String> failedToRestartCloudInstance = request.getCloudInstances().stream()
                    .map(CloudInstance::getInstanceId)
                    .filter(instanceId -> !successfullyStartedCloudInstance.contains(instanceId))
                    .collect(Collectors.toList());
            rollingVerticalScaleService.finishStartInstances(request.getResourceId(), successfullyStartedCloudInstance, rollingVerticalScaleResult.getGroup());
            if (!failedToRestartCloudInstance.isEmpty()) {
                rollingVerticalScaleService.failedStartInstances(request.getResourceId(),
                        failedToRestartCloudInstance, rollingVerticalScaleResult.getGroup(), "");
            }
            LOGGER.info("Started instances. Result: Successfully Started:[{}]. Failed to Start:[{}]",
                    successfullyStartedCloudInstance, failedToRestartCloudInstance);

            return new RollingVerticalScaleStartInstancesResult(request.getResourceId(), rollingVerticalScaleResult);
        } catch (Exception e) {
            List<String> instanceIds = request.getCloudInstances().stream().map(CloudInstance::getInstanceId).toList();
            String message = "Failed while attempting to start instances: " + instanceIds;
            LOGGER.error(message, e);
            rollingVerticalScaleService.failedStartInstances(request.getResourceId(), instanceIds, rollingVerticalScaleResult.getGroup(), e.getMessage());
            return new RollingVerticalScaleStartInstancesResult(request.getResourceId(), rollingVerticalScaleResult);
        }
    }

    private List<CloudVmInstanceStatus> startInstances(CloudConnector connector, AuthenticatedContext ac,
            List<CloudInstance> instancesToStart, List<CloudResource> cloudResources) {
        try {
            List<CloudVmInstanceStatus> startedCloudInstance = connector.instances()
                    .startWithLimitedRetry(ac,  cloudResources, instancesToStart, RESTART_POLL_TIMEBOUND_MS);
            return startedCloudInstance.stream().filter(i -> i.getStatus().equals(InstanceStatus.STARTED)).collect(Collectors.toList());
        } catch (PollerStoppedException p) {
            LOGGER.warn("Timed out while attempting to start instances. Attempting to get states for attempted nodes", p);
            return getInstanceStatusOnStartError(connector, ac, instancesToStart, p);
        } catch (Exception e) {
            LOGGER.warn("Exception while attempting to start instances. Attempting to get states for attempted nodes", e);
            return getInstanceStatusOnStartError(connector, ac, instancesToStart, e);
        }
    }

    private List<CloudVmInstanceStatus> getInstanceStatusOnStartError(CloudConnector connector, AuthenticatedContext ac,
            List<CloudInstance> instancesToRestart, Exception originalException) {
        try {
            List<CloudVmInstanceStatus> instanceStatuses = connector.instances().checkWithoutRetry(ac, instancesToRestart);
            return instanceStatuses.stream().filter(i -> i.getStatus().equals(InstanceStatus.STARTED)).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.warn("Error while trying to get instance status after start failure. Propagating original error from the start attempt", e);
            String message = "Error while attempting to start instances";
            if (originalException instanceof PollerStoppedException) {
                message = "Timed out while waiting for instances to start";
            }
            throw new CloudbreakRuntimeException(message, originalException);
        }
    }

    private void updateRollingVerticalScaleResult(RollingVerticalScaleResult result, List<CloudVmInstanceStatus> statuses) {
        for (CloudVmInstanceStatus status : statuses) {
            String instanceId = status.getCloudInstance().getInstanceId();
            RollingVerticalScaleStatus prevStatus = result.getStatus(instanceId).getStatus();
            if (prevStatus.equals(RollingVerticalScaleStatus.SCALED)) {
                if (status.getStatus().equals(InstanceStatus.STARTED)) {
                    result.setStatus(instanceId, RollingVerticalScaleStatus.SUCCESS);
                } else {
                    result.setStatus(instanceId, RollingVerticalScaleStatus.RESTART_FAILED);
                }
            } else if (prevStatus.equals(RollingVerticalScaleStatus.SCALING_FAILED) && !status.getStatus().equals(InstanceStatus.STARTED)) {
                result.setStatus(instanceId, RollingVerticalScaleStatus.SCALING_RESTART_FAILED);
            }
        }
    }
}

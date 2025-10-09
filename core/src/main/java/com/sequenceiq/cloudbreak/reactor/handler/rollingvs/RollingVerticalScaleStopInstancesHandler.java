package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import java.util.ArrayList;
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
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStopInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStopInstancesResult;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RollingVerticalScaleStopInstancesHandler extends ExceptionCatcherEventHandler<RollingVerticalScaleStopInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleStopInstancesHandler.class);

    private static final Long RESTART_POLL_TIMEBOUND_MS = 1_200_000L;

    @Inject
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollingVerticalScaleStopInstancesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollingVerticalScaleStopInstancesRequest> event) {
        return new StackFailureEvent(RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FAILURE_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollingVerticalScaleStopInstancesRequest> event) {
        LOGGER.info("RollingVerticalScaleStopInstancesHandler: {}", event.getData().getResourceId());
        RollingVerticalScaleStopInstancesRequest request = event.getData();
        RollingVerticalScaleResult rollingVerticalScaleResult = request.getRollingVerticalScaleResult();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext authenticatedContext = connector.authentication().authenticate(cloudContext, request.getCloudCredential());

            List<CloudInstance> targetCloudInstances = getTargetCloudInstances(connector, authenticatedContext,
                    rollingVerticalScaleResult, request.getCloudInstances(), request.getTargetInstanceType());
            List<String> targetInstanceIds = targetCloudInstances.stream().map(CloudInstance::getInstanceId).toList();
            rollingVerticalScaleService.stopInstances(request.getResourceId(), targetInstanceIds, rollingVerticalScaleResult.getGroup());
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = stopInstances(connector, authenticatedContext,
                    targetCloudInstances, request.getCloudResources());
            updateRollingVerticalScaleResult(rollingVerticalScaleResult, cloudVmInstanceStatuses);

            List<String> successfullyStoppedCloudInstance = cloudVmInstanceStatuses.stream()
                    .map(CloudVmInstanceStatus::getCloudInstance)
                    .map(CloudInstance::getInstanceId)
                    .collect(Collectors.toList());
            List<String> failedToStopCloudInstance = targetInstanceIds.stream().filter(i -> !successfullyStoppedCloudInstance.contains(i))
                    .collect(Collectors.toList());
            rollingVerticalScaleService.finishStopInstances(request.getResourceId(), successfullyStoppedCloudInstance, rollingVerticalScaleResult.getGroup());
            rollingVerticalScaleService.failedToStopInstance(request.getResourceId(), failedToStopCloudInstance, rollingVerticalScaleResult.getGroup(), "");
            LOGGER.info("Stopped instances. Result: Successfully Stopped:[{}]. Failed to Stop:[{}]", successfullyStoppedCloudInstance,
                    failedToStopCloudInstance);
            return new RollingVerticalScaleStopInstancesResult(request.getResourceId(), rollingVerticalScaleResult);
        } catch (Exception e) {
            List<String> instanceIds = request.getCloudInstances().stream().map(CloudInstance::getInstanceId).toList();
            rollingVerticalScaleService.failedToStopInstance(request.getResourceId(), instanceIds, rollingVerticalScaleResult.getGroup(), e.getMessage());
            String message = String.format("Failed while attempting to stop instances: [%s] : resourceId: %s", instanceIds, request.getResourceId());
            LOGGER.error(message, e);
            return new RollingVerticalScaleStopInstancesResult(request.getResourceId(), rollingVerticalScaleResult);
        }
    }

    private List<CloudVmInstanceStatus> stopInstances(CloudConnector connector, AuthenticatedContext ac,
                                                    List<CloudInstance> instancesToStop, List<CloudResource> cloudResources) {
        try {
            List<CloudVmInstanceStatus> stoppedCloudInstances = connector.instances()
                    .stopWithLimitedRetry(ac,  cloudResources, instancesToStop, RESTART_POLL_TIMEBOUND_MS);
            return stoppedCloudInstances.stream().filter(i -> i.getStatus().equals(InstanceStatus.STOPPED)).collect(Collectors.toList());
        } catch (PollerStoppedException p) {
            LOGGER.warn("Timed out while attempting to stop instances. Attempting to get states for attempted nodes", p);
            return getInstanceStatusOnStopError(connector, ac, instancesToStop, p);
        } catch (Exception e) {
            LOGGER.warn("Exception while attempting to stop instances. Attempting to get states for attempted nodes", e);
            return getInstanceStatusOnStopError(connector, ac, instancesToStop, e);
        }
    }

    private List<CloudVmInstanceStatus> getInstanceStatusOnStopError(CloudConnector connector, AuthenticatedContext ac,
                                                                    List<CloudInstance> instancesToStop, Exception originalException) {
        try {
            List<CloudVmInstanceStatus> instanceStatuses = connector.instances().checkWithoutRetry(ac, instancesToStop);
            return instanceStatuses.stream().filter(i -> i.getStatus().equals(InstanceStatus.STOPPED)).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.warn("Error while trying to get instance status after stop failure. Propagating original error from the stop attempt", e);
            String message = "Error while attempting to stop instances";
            if (originalException instanceof PollerStoppedException) {
                message = "Timed out while waiting for instances to stop";
            }
            throw new CloudbreakRuntimeException(message, originalException);
        }
    }

    private void updateRollingVerticalScaleResult(RollingVerticalScaleResult result, List<CloudVmInstanceStatus> instanceStatuses) {
        for (CloudVmInstanceStatus status : instanceStatuses) {
            String instanceId = status.getCloudInstance().getInstanceId();
            if (status.getStatus().equals(InstanceStatus.STOPPED)) {
                result.setStatus(instanceId, RollingVerticalScaleStatus.STOPPED);
            } else {
                result.setStatus(instanceId, RollingVerticalScaleStatus.STOP_FAILED, status.getStatusReason());
            }
        }
    }

    private List<CloudInstance> getTargetCloudInstances(CloudConnector connector, AuthenticatedContext ac, RollingVerticalScaleResult result,
            List<CloudInstance> cloudInstances, String targetInstanceType) {
        List<String> instanceIds = cloudInstances.stream().map(CloudInstance::getInstanceId).toList();
        InstanceTypeMetadata instanceTypeMetadata = connector.metadata().collectInstanceTypes(ac, instanceIds);
        List<CloudInstance> targetInstanceIds = new ArrayList<>();
        for (CloudInstance cloudInstance : cloudInstances) {
            String instanceType = instanceTypeMetadata.getInstanceTypes().get(cloudInstance.getInstanceId());
            if (instanceType.equals(targetInstanceType)) {
                LOGGER.debug("Skipping vertical scaling for instance {} with instance type {} as it already matches the target type.",
                        cloudInstance.getInstanceId(), targetInstanceType);
                result.setStatus(cloudInstance.getInstanceId(), RollingVerticalScaleStatus.SUCCESS);
            } else {
                targetInstanceIds.add(cloudInstance);
            }
        }
        return targetInstanceIds;
    }
}

package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleStatus;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesResult;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
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

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private RuntimeVersionService runtimeVersionService;

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
                    .map(CloudInstance::getInstanceId).toList();
            List<String> failedToRestartCloudInstance = request.getCloudInstances().stream()
                    .map(CloudInstance::getInstanceId)
                    .filter(instanceId -> !successfullyStartedCloudInstance.contains(instanceId)).toList();
            rollingVerticalScaleService.finishStartInstances(request.getResourceId(), successfullyStartedCloudInstance, rollingVerticalScaleResult.getGroup());
            if (!failedToRestartCloudInstance.isEmpty()) {
                rollingVerticalScaleService.failedStartInstances(request.getResourceId(),
                        failedToRestartCloudInstance, rollingVerticalScaleResult.getGroup(), "");
            }
            LOGGER.info("Started instances. Result: Successfully Started:[{}]. Failed to Start:[{}]",
                    successfullyStartedCloudInstance, failedToRestartCloudInstance);

            if (!successfullyStartedCloudInstance.isEmpty()) {
                waitForServicesHealthy(request.getResourceId(), successfullyStartedCloudInstance, rollingVerticalScaleResult);
            }

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
            return startedCloudInstance.stream().filter(i -> i.getStatus().equals(InstanceStatus.STARTED)).toList();
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
            return instanceStatuses.stream().filter(i -> i.getStatus().equals(InstanceStatus.STARTED)).toList();
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

    private void waitForServicesHealthy(Long stackId, List<String> instanceIds, RollingVerticalScaleResult rollingVerticalScaleResult) {
        try {
            LOGGER.info("Waiting for services to be healthy on instances: {}", instanceIds);
            StackDto stackDto = stackDtoService.getById(stackId);

            Set<InstanceMetadataView> instancesToWaitFor = stackDto.getAllAvailableInstances().stream()
                    .filter(instance -> instanceIds.contains(instance.getInstanceId()))
                    .filter(instance -> instance.getDiscoveryFQDN() != null)
                    .collect(Collectors.toSet());
            List<String> instanceIdsToWaitFor = instancesToWaitFor.stream().map(InstanceMetadataView::getInstanceId).toList();

            if (instancesToWaitFor.isEmpty()) {
                LOGGER.warn("No instances with FQDN found for instance IDs: {}. Skipping service health check.", instanceIds);
                return;
            }

            LOGGER.debug("Waiting for services to be healthy on {} instances: {}", instancesToWaitFor.size(),
                    instancesToWaitFor.stream().map(InstanceMetadataView::getDiscoveryFQDN).toList());
            rollingVerticalScaleService.waitingForServicesHealthy(stackId, rollingVerticalScaleResult.getGroup(), instanceIdsToWaitFor);

            ClusterStatusService clusterStatusService = clusterApiConnectors.getConnector(stackDto).clusterStatusService();
            Long clusterId = stackDto.getCluster() != null ? stackDto.getCluster().getId() : null;
            Optional<String> runtimeVersion = runtimeVersionService.getRuntimeVersion(clusterId);
            ExtendedPollingResult pollingResult = clusterStatusService.waitForHostHealthyServices(new HashSet<>(instancesToWaitFor), runtimeVersion);

            if (!pollingResult.isSuccess()) {
                Set<Long> failedHostIds = pollingResult.getFailedInstancePrivateIds();
                List<String> failedInstanceIds = instancesToWaitFor.stream()
                        .filter(instance -> failedHostIds.contains(instance.getPrivateId()))
                        .map(InstanceMetadataView::getInstanceId).toList();
                LOGGER.warn("Waiting for services to be healthy timed out or failed for instances: {}. Flow execution continues.",
                        failedInstanceIds);
                updateRollingVerticalScaleResultWithNonHealthyServices(stackId, rollingVerticalScaleResult, failedInstanceIds);
            } else {
                LOGGER.info("All services are healthy on instances: {}",
                        instancesToWaitFor.stream().map(InstanceMetadataView::getDiscoveryFQDN).toList());
                rollingVerticalScaleService.updateInstancesToServicesHealthy(stackId, instancesToWaitFor);
            }
        } catch (ClusterClientInitException e) {
            LOGGER.warn("Failed to initialize cluster client for service health check. Flow execution continues.", e);
        } catch (Exception e) {
            LOGGER.warn("Unexpected error while waiting for services to be healthy. Flow execution continues.", e);
        }
    }

    private void updateRollingVerticalScaleResultWithNonHealthyServices(Long stackId,
            RollingVerticalScaleResult rollingVerticalScaleResult, List<String> failedInstanceIds) {
        for (String instanceId : failedInstanceIds) {
            rollingVerticalScaleResult.setStatus(instanceId, RollingVerticalScaleStatus.SERVICES_UNHEALTHY);
        }
        rollingVerticalScaleService.updateInstancesToServiceUnhealthy(stackId, rollingVerticalScaleResult.getGroup(), failedInstanceIds);
    }
}

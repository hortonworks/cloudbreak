package com.sequenceiq.cloudbreak.reactor.handler.rollingvs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleInstancesResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RollingVerticalScaleInstancesHandler extends ExceptionCatcherEventHandler<RollingVerticalScaleInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleInstancesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private StackUpscaleService stackUpscaleService;

    @Inject
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Inject
    private CoreVerticalScaleService coreVerticalScaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollingVerticalScaleInstancesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollingVerticalScaleInstancesRequest> event) {
        LOGGER.error("Unexpected exception occurred in RollingVerticalScaleInstancesHandler for resourceId: {}", resourceId, e);
        return new StackFailureEvent(RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FAILURE_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollingVerticalScaleInstancesRequest> event) {
        LOGGER.info("RollingVerticalScaleInstancesHandler: {}", event.getData().getResourceId());
        RollingVerticalScaleInstancesRequest request = event.getData();
        RollingVerticalScaleResult rollingVerticalScaleResult = request.getRollingVerticalScaleResult();
        StackVerticalScaleV4Request stackVerticalScaleV4Request = request.getStackVerticalScaleV4Request();
        CloudContext cloudContext = request.getCloudContext();
        String group = stackVerticalScaleV4Request.getGroup();
        String requestedInstanceType = stackVerticalScaleV4Request.getTemplate().getInstanceType();

        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request.getCloudCredential(), cloudContext, connector);
            List<CloudResourceStatus> resourceStatus = stackUpscaleService.verticalScale(ac, request, connector, group);
            LOGGER.info("Vertical scaling resource statuses: {}", resourceStatus);

            Map<String, String> updatedInstanceType = getInstancetypeMetadata(connector, ac, request.getCloudResources());
            updateRollingVerticalScaleResult(request.getResourceId(), rollingVerticalScaleResult,
                    updatedInstanceType, resourceStatus, requestedInstanceType, stackVerticalScaleV4Request);

            InstanceStoreMetadata instanceStoreMetadata =  stackUpscaleService.getInstanceStorageInfo(ac,
                    requestedInstanceType, connector);
            Integer instanceStorageCount = instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled(requestedInstanceType);
            Integer instanceStorageSize = instanceStoreMetadata.mapInstanceTypeToInstanceSizeNullHandled(requestedInstanceType);
            coreVerticalScaleService.updateTemplateWithVerticalScaleInformation(request.getResourceId(),
                    stackVerticalScaleV4Request, instanceStorageCount, instanceStorageSize);

            RollingVerticalScaleInstancesResult result = new RollingVerticalScaleInstancesResult(
                    request.getResourceId(),
                    rollingVerticalScaleResult);
            LOGGER.debug("Vertical scaling successfully finished for {}, and the result is: {}", cloudContext, result);
            return result;
        } catch (Exception e) {
            LOGGER.error("Vertical scaling stack failed", e);
            updateRollingVerticalScaleResultOnError(request.getResourceId(), rollingVerticalScaleResult,
                    request.getCloudResources(), e.getMessage(), stackVerticalScaleV4Request);
            return new RollingVerticalScaleInstancesResult(request.getResourceId(), rollingVerticalScaleResult);
        }
    }

    private AuthenticatedContext getAuthenticatedContext(CloudCredential cloudCredential, CloudContext cloudContext, CloudConnector connector) {
        return connector.authentication().authenticate(cloudContext, cloudCredential);
    }

    private void updateRollingVerticalScaleResult(Long stackId, RollingVerticalScaleResult result, Map<String, String> updatedInstanceType,
            List<CloudResourceStatus> cloudResourceStatuses, String requestedInstanceType, StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        List<String> successfullyScaledInstances = new ArrayList<>();
        List<String> failedInstances = new ArrayList<>();
        for (CloudResourceStatus status : cloudResourceStatuses) {
            String instanceId = status.getCloudResource().getInstanceId();
            if (updatedInstanceType.containsKey(instanceId) && updatedInstanceType.get(instanceId).equals(requestedInstanceType)) {
                result.setStatus(instanceId, RollingVerticalScaleStatus.SCALED);
                successfullyScaledInstances.add(instanceId);
            } else {
                result.setStatus(instanceId, RollingVerticalScaleStatus.SCALING_FAILED, status.getStatusReason());
                failedInstances.add(instanceId);
            }
        }
        LOGGER.info("Vertical scale successful instances: count={}, instanceIds=[{}]. Vertical scale failed instances: count={}, instanceIds=[{}].",
                successfullyScaledInstances.size(), successfullyScaledInstances, failedInstances.size(), failedInstances);
        rollingVerticalScaleService.finishVerticalScaleInstances(stackId, successfullyScaledInstances, stackVerticalScaleV4Request);
        rollingVerticalScaleService.failedVerticalScaleInstances(stackId, failedInstances, stackVerticalScaleV4Request, "");
    }

    private Map<String, String> getInstancetypeMetadata(CloudConnector connector, AuthenticatedContext ac, List<CloudResource> cloudResources) {
        List<String> instanceIds = cloudResources.stream().map(CloudResource::getInstanceId).toList();
        InstanceTypeMetadata instanceTypeMetadata = connector.metadata().collectInstanceTypes(ac, instanceIds);
        return instanceTypeMetadata.getInstanceTypes();
    }

    private void updateRollingVerticalScaleResultOnError(Long stackId, RollingVerticalScaleResult result,
            List<CloudResource> cloudResources, String errorMessage, StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        List<String> instanceIds = cloudResources.stream().map(CloudResource::getInstanceId).toList();
        for (String instanceId : instanceIds) {
            result.setStatus(instanceId, RollingVerticalScaleStatus.SCALING_FAILED, errorMessage);
        }
        rollingVerticalScaleService.failedVerticalScaleInstances(stackId, instanceIds, stackVerticalScaleV4Request, errorMessage);
    }
}



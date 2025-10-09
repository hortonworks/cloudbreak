package com.sequenceiq.cloudbreak.reactor;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStoreMetadata;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleResult;

@Component
public class CoreVerticalScaleHandler implements CloudPlatformEventHandler<CoreVerticalScaleRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreVerticalScaleHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private StackUpscaleService stackUpscaleService;

    @Override
    public Class<CoreVerticalScaleRequest> type() {
        return CoreVerticalScaleRequest.class;
    }

    @Override
    public void accept(Event<CoreVerticalScaleRequest> stackVerticalScaleRequestEvent) {
        LOGGER.debug("Received event: {}", stackVerticalScaleRequestEvent);
        CoreVerticalScaleRequest<CoreVerticalScaleResult> request = stackVerticalScaleRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        StackVerticalScaleV4Request stackVerticalScaleV4Request = request.getStackVerticalScaleV4Request();
        String requestedInstanceType = stackVerticalScaleV4Request.getTemplate().getInstanceType();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request, cloudContext, connector);
            List<CloudResourceStatus> resourceStatus = stackUpscaleService.verticalScale(ac, request, connector,
                        stackVerticalScaleV4Request.getGroup());
            LOGGER.info("Vertical scaling resource statuses: {}", resourceStatus);
            InstanceStoreMetadata instanceStoreMetadata =  stackUpscaleService.getInstanceStorageInfo(ac,
                    requestedInstanceType, connector);
            Integer instanceStorageCount = instanceStoreMetadata.mapInstanceTypeToInstanceStoreCountNullHandled(requestedInstanceType);
            Integer instanceStorageSize = instanceStoreMetadata.mapInstanceTypeToInstanceSizeNullHandled(requestedInstanceType);
            CoreVerticalScaleResult result = new CoreVerticalScaleResult(
                    request.getResourceId(),
                    ResourceStatus.UPDATED,
                    resourceStatus,
                    stackVerticalScaleV4Request,
                    instanceStorageCount,
                    instanceStorageSize);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(stackVerticalScaleRequestEvent.getHeaders(), result));
            LOGGER.debug("Vertical scaling successfully finished for {}, and the result is: {}", cloudContext, result);
        } catch (Exception e) {
            LOGGER.error("Vertical scaling stack failed", e);
            CoreVerticalScaleResult result = new CoreVerticalScaleResult(
                    e.getMessage(),
                    e,
                    request.getResourceId(),
                    request.getStackVerticalScaleV4Request());
            request.getResult().onNext(result);
            eventBus.notify(CloudPlatformResult.failureSelector(CoreVerticalScaleResult.class),
                    new Event<>(stackVerticalScaleRequestEvent.getHeaders(), result));
        }
    }

    private AuthenticatedContext getAuthenticatedContext(CoreVerticalScaleRequest<CoreVerticalScaleResult> request,
            CloudContext cloudContext, CloudConnector connector) {
        return connector.authentication().authenticate(cloudContext, request.getCloudCredential());
    }

}

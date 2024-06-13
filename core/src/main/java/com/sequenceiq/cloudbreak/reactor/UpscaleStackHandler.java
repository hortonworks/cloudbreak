package com.sequenceiq.cloudbreak.reactor;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudImageException;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackImageFallbackResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackResult;

@Component
public class UpscaleStackHandler implements CloudPlatformEventHandler<UpscaleStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private StackUpscaleService stackUpscaleService;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    public Class<UpscaleStackRequest> type() {
        return UpscaleStackRequest.class;
    }

    @Override
    public void accept(Event<UpscaleStackRequest> upscaleStackRequestEvent) {
        LOGGER.debug("Received event: {}", upscaleStackRequestEvent);
        UpscaleStackRequest<UpscaleStackResult> request = upscaleStackRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request, cloudContext, connector);
            List<CloudResourceStatus> resourceStatus = stackUpscaleService.upscale(ac, request, connector);
            addToOldLBIfMigration(request, cloudContext, connector, request.getCloudStack(), resourceStatus, request.isMigrationNeed());
            LOGGER.info("Upscaled resource statuses: {}", resourceStatus);
            UpscaleStackResult result = new UpscaleStackResult(request.getResourceId(), ResourceStatus.UPDATED, resourceStatus);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(upscaleStackRequestEvent.getHeaders(), result));
            LOGGER.debug("Upscale successfully finished for {}, and the result is: {}", cloudContext, result);
        } catch (CloudImageException e) {
            UpscaleStackImageFallbackResult result = new UpscaleStackImageFallbackResult(
                    request.getResourceId(), ResourceStatus.FAILED, List.of(), e.getMessage());
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(upscaleStackRequestEvent.getHeaders(), result));
            LOGGER.debug("Marketplace image error, attempt to fallback to vhd image {}", cloudContext, e);
        } catch (Exception e) {
            LOGGER.error("Upscaling stack failed", e);
            UpscaleStackResult result = new UpscaleStackResult(e.getMessage(), e, request.getResourceId());
            request.getResult().onNext(result);
            eventBus.notify(CloudPlatformResult.failureSelector(UpscaleStackResult.class), new Event<>(upscaleStackRequestEvent.getHeaders(), result));
        }
    }

    private void addToOldLBIfMigration(UpscaleStackRequest<UpscaleStackResult> request, CloudContext cloudContext, CloudConnector connector,
            CloudStack cloudStack, List<CloudResourceStatus> resourceStatus, boolean migrationNeed) {
        if (migrationNeed) {
            LOGGER.info("The cluster is under migration, so the old LB need to populate as well. The migration flow will populate the new LB only");
            LOGGER.debug("We create a new context and authenticated context with AWS platform variant");
            CloudContext newCloudContext = cloudContext.createPrototype()
                    .withVariant(AwsConstants.AwsVariant.AWS_VARIANT.variant())
                    .build();
            AuthenticatedContext ac = getAuthenticatedContext(request, newCloudContext, connector);
            List<CloudResource> newInstances = resourceStatus.stream().map(CloudResourceStatus::getCloudResource).collect(Collectors.toList());
            LOGGER.debug("Update old LBs with new instances: {}", newInstances);
            for (CloudLoadBalancer loadBalancer : cloudStack.getLoadBalancers()) {
                cfStackUtil.addLoadBalancerTargets(ac, loadBalancer, newInstances);
            }
            LOGGER.debug("LB successfully updated");
        } else {
            LOGGER.info("The cluster doesn't need to migrate, the upscale populate the proper LB");
        }
    }

    private AuthenticatedContext getAuthenticatedContext(UpscaleStackRequest<UpscaleStackResult> request, CloudContext cloudContext,
            CloudConnector connector) {
        return connector.authentication().authenticate(cloudContext, request.getCloudCredential());
    }

}
package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.handler;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.FreeIpaRollingVerticalScaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleDescribeRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleDescribeResult;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class RollingVerticalScaleDescribeHandler extends ExceptionCatcherEventHandler<RollingVerticalScaleDescribeRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleDescribeHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollingVerticalScaleDescribeRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollingVerticalScaleDescribeRequest> event) {
        return new FreeIpaRollingVerticalScaleFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollingVerticalScaleDescribeRequest> event) {
        RollingVerticalScaleDescribeRequest request = event.getData();
        FreeIpaVerticalScaleParameters scaleConfig = request.getScaleConfig();
        String instanceId = request.getInstanceId();
        String actualInstanceType = collectActualInstanceType(request.getResourceId(), instanceId, scaleConfig.getOriginalInstanceType());
        if (scaleConfig.getTargetInstanceType().equals(actualInstanceType)) {
            LOGGER.info("Instance {} already at target type {}, skipping rolling vertical scale for this instance", instanceId, actualInstanceType);
            return new StackEvent(FreeIpaRollingVerticalScaleEvent.ROLLING_VERTICAL_SCALE_DESCRIBE_SKIP_EVENT.event(), request.getResourceId());
        }
        LOGGER.debug("Instance {} at actual type {}, will scale to {}", instanceId, actualInstanceType, scaleConfig.getTargetInstanceType());
        return new RollingVerticalScaleDescribeResult(request.getResourceId(), scaleConfig.withOriginalInstanceType(actualInstanceType));
    }

    private String collectActualInstanceType(Long stackId, String instanceId, String fallback) {
        try {
            Stack stack = stackService.getStackById(stackId);
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            CloudContext cloudContext = CloudContext.Builder.builder()
                    .withId(stack.getId())
                    .withName(stack.getName())
                    .withCrn(stack.getResourceCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withVariant(stack.getPlatformvariant())
                    .withLocation(location)
                    .withUserName(stack.getOwner())
                    .withAccountId(stack.getAccountId())
                    .build();
            CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            String actualType = connector.metadata().collectInstanceTypes(ac, List.of(instanceId))
                    .getInstanceTypes().getOrDefault(instanceId, fallback);
            LOGGER.debug("Cloud provider reports instance {} at type {}", instanceId, actualType);
            return actualType;
        } catch (Exception e) {
            LOGGER.warn("Failed to describe instance type for {} from cloud provider, using fallback type {}", instanceId, fallback, e);
            return fallback;
        }
    }
}

package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.handler;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleHealthCheckRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleHealthCheckResult;
import com.sequenceiq.freeipa.flow.stack.start.AttemptMakerFactory;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class RollingVerticalScaleHealthCheckHandler extends ExceptionCatcherEventHandler<RollingVerticalScaleHealthCheckRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleHealthCheckHandler.class);

    @Value("${freeipa.rolling-vertical-scale.health-check.max-attempts:60}")
    private int maxAttempts;

    @Value("${freeipa.rolling-vertical-scale.health-check.interval-seconds:10}")
    private int sleepingTimeSec;

    @Value("${freeipa.rolling-vertical-scale.health-check.consecutive-success-required:3}")
    private int consecutiveSuccessRequired;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private AttemptMakerFactory attemptMakerFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RollingVerticalScaleHealthCheckRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RollingVerticalScaleHealthCheckRequest> event) {
        return new FreeIpaRollingVerticalScaleFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RollingVerticalScaleHealthCheckRequest> event) {
        RollingVerticalScaleHealthCheckRequest request = event.getData();
        Long stackId = request.getResourceId();
        String instanceId = request.getInstanceId();

        LOGGER.info("Health checking instance {} after rolling vertical scale (maxAttempts={}, interval={}s, consecutiveSuccess={})",
                instanceId, maxAttempts, sleepingTimeSec, consecutiveSuccessRequired);
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        InstanceMetaData instance = instanceMetaDataService.getByInstanceIds(stackId, List.of(instanceId))
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Instance not found: " + instanceId));

        boolean healthy = pollInstanceHealth(stack, instance);

        if (!healthy) {
            LOGGER.warn("Instance {} is unhealthy after resize, failing flow", instanceId);
            return new FreeIpaRollingVerticalScaleFailureEvent(stackId,
                    new CloudbreakServiceException(String.format("FreeIPA instance '%s' (host: %s) did not pass health check within %d seconds "
                            + "after vertical scale. Check FreeIPA service logs on the instance and retry the operation if the issue "
                            + "is transient.", instanceId, instance.getDiscoveryFQDN(), maxAttempts * sleepingTimeSec)));
        }
        return new RollingVerticalScaleHealthCheckResult(stackId, healthy);
    }

    private boolean pollInstanceHealth(Stack stack, InstanceMetaData instance) {
        try {
            Polling.stopAfterAttempt(maxAttempts)
                    .stopIfException(false)
                    .waitPeriodly(sleepingTimeSec, TimeUnit.SECONDS)
                    .run(attemptMakerFactory.create(stack, Set.of(instance), consecutiveSuccessRequired));
            LOGGER.info("Instance {} is healthy after rolling vertical scale", instance.getInstanceId());
            return true;
        } catch (Exception e) {
            LOGGER.warn("Instance {} did not become healthy within polling window: {}", instance.getInstanceId(), e.getMessage());
            return false;
        }
    }
}

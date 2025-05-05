package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stophealthagent.StopHealthAgentRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.healthagent.HealthAgentService;

@Component
public class StopHealthAgentHandler extends ExceptionCatcherEventHandler<StopHealthAgentRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopHealthAgentHandler.class);

    private static final long WAIT_FOR_LB_DELAY = 60L;

    @Inject
    private HealthAgentService healthAgentService;

    @Inject
    private DelayedExecutorService delayedExecutorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StopHealthAgentRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StopHealthAgentRequest> event) {
        return new DownscaleFailureEvent(resourceId, "Stopping health agent", Set.of(), Map.of(), e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<StopHealthAgentRequest> event) {
        StopHealthAgentRequest request = event.getData();
        LOGGER.info("Stop health agents gracefully on {}", request.getFqdns());
        if (request.getFqdns() != null && !request.getFqdns().isEmpty()) {
            healthAgentService.stopHealthAgentOnHosts(request.getResourceId(), Set.copyOf(request.getFqdns()));
            try {
                delayedExecutorService.runWithDelay(() -> LOGGER.debug("Waiting for LoadBalancer to realize instance down state is done"),
                        WAIT_FOR_LB_DELAY, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return new StackEvent(DownscaleFlowEvent.STOP_HEALTH_AGENT_FINISHED.event(), request.getResourceId());
    }
}

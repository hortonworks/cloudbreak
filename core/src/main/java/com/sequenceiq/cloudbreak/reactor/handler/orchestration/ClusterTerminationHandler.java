package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus.Value.DELETED;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;
import com.sequenceiq.cloudbreak.service.metering.MeteringService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class ClusterTerminationHandler implements EventHandler<ClusterTerminationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private MeteringService meteringService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterTerminationRequest.class);
    }

    @Override
    public void accept(Event<ClusterTerminationRequest> event) {
        ClusterTerminationRequest request = event.getData();
        ClusterTerminationResult result = new ClusterTerminationResult(request);
        try {
            StackDto stack = stackDtoService.getByIdWithoutResources(request.getStackId());
            meteringService.unscheduleSync(request.getStackId());
            meteringService.sendMeteringStatusChangeEventForStack(stack, DELETED);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to delete cluster containers", e);
            if (!event.getData().isForced()) {
                LOGGER.debug("Ignoring error during deleting cluster containers because forced termination flag.");
                result = new ClusterTerminationResult(e.getMessage(), e, request);
            }
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));

    }
}

package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_FALLBACK;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_FALLBACK_EXHAUSTED;

import java.util.List;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.InstanceTypeFallbackEvent;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.StackView;

@Component("instanceTypeFallbackNotificationHandler")
public class InstanceTypeFallbackNotificationHandler implements Consumer<Event<InstanceTypeFallbackEvent>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTypeFallbackNotificationHandler.class);

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private EventBus eventBus;

    @PostConstruct
    void register() {
        eventBus.on(InstanceTypeFallbackEvent.INSTANCE_TYPE_FALLBACK_SELECTOR, this);
    }

    @Override
    public void accept(Event<InstanceTypeFallbackEvent> event) {
        InstanceTypeFallbackEvent data = event.getData();
        Long stackId = data.getCloudContext().getId();
        if (stackId == null) {
            LOGGER.warn("Cannot fire instance-type fallback notification without a stack id: {}", data);
            return;
        }
        String eventType = resolveEventType(stackId);
        ResourceEvent resourceEvent = data.isExhausted() ? STACK_INSTANCE_TYPE_FALLBACK_EXHAUSTED : STACK_INSTANCE_TYPE_FALLBACK;
        List<String> args = data.isExhausted()
                ? List.of(nullSafe(data.getInstanceGroup()), nullSafe(data.getOriginalType()), nullSafe(data.getReasonSummary()))
                : List.of(nullSafe(data.getInstanceGroup()), nullSafe(data.getOriginalType()), nullSafe(data.getReasonSummary()),
                        nullSafe(data.getFallbackType()));
        LOGGER.info("Firing {} notification for stack {}, group {}, eventType {}, args {}",
                resourceEvent, stackId, data.getInstanceGroup(), eventType, args);
        cloudbreakEventService.fireCloudbreakInstanceGroupEvent(stackId, eventType, data.getInstanceGroup(), resourceEvent, args);
    }

    private String resolveEventType(Long stackId) {
        try {
            StackView stack = stackDtoService.getStackViewById(stackId);
            if (stack != null && stack.getStatus() != null) {
                return stack.getStatus().name();
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Could not resolve current status for stack {}; using UNKNOWN.", stackId, e);
        }
        return "UNKNOWN";
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

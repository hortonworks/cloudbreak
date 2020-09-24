package com.sequenceiq.environment.environment.flow.config.update.handler;

import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesEvent;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesEvent.EnvStackConfigUpdatesEventBuilder;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesFailedEvent;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesHandlerSelectors;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors;
import com.sequenceiq.environment.environment.service.stack.StackPollerService;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import org.springframework.stereotype.Component;
import reactor.bus.Event;

@Component
public class StackConfigUpdatesHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private final StackPollerService stackPollerService;

    public StackConfigUpdatesHandler(EventSender eventSender,
        StackPollerService stackPollerService) {
        super(eventSender);
        this.stackPollerService = stackPollerService;
    }

    @Override
    public String selector() {
        return EnvStackConfigUpdatesHandlerSelectors.STACK_CONFIG_UPDATES_HANDLER_EVENT.event();
    }

    @Override
    public void accept(Event<EnvironmentDto> event) {
        try {
            stackPollerService.updateStackConfigurations(event.getData().getResourceId(),
                event.getData().getResourceCrn(), event.getHeaders().get(FlowConstants.FLOW_ID));

            EnvStackConfigUpdatesEvent envStackConfigUpdatesEvent = EnvStackConfigUpdatesEventBuilder
                .anEnvStackConfigUpdatesEvent()
                .withSelector(
                    EnvStackConfigUpdatesStateSelectors.FINISH_ENV_STACK_CONFIG_UPDATES_EVENT
                        .selector())
                .withResourceId(event.getData().getResourceId())
                .build();

            eventSender().sendEvent(envStackConfigUpdatesEvent, event.getHeaders());
        } catch (Exception e) {
            eventSender().sendEvent(new EnvStackConfigUpdatesFailedEvent(event.getData(), e, null),
                event.getHeaders());
        }
    }
}

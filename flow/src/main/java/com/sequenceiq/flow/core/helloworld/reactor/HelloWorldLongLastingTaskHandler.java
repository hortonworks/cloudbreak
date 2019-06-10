package com.sequenceiq.flow.core.helloworld.reactor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.helloworld.HelloWorldSelectableEvent;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldLongLastingTaskFailureResponse;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldLongLastingTaskSuccessResponse;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class HelloWorldLongLastingTaskHandler implements EventHandler<HelloWorldLongLastingTaskTriggerEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldLongLastingTaskHandler.class);

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return HelloWorldSelectableEvent.selector(HelloWorldLongLastingTaskTriggerEvent.class);
    }

    @Override
    public void accept(Event<HelloWorldLongLastingTaskTriggerEvent> event) {
        HelloWorldLongLastingTaskTriggerEvent helloWorldReactorEvent = event.getData();
        Long resourceId = helloWorldReactorEvent.getResourceId();
        Selectable response;
        try {
            LOGGER.info("Long lasting task execution...");
            response = new HelloWorldLongLastingTaskSuccessResponse(resourceId);
        } catch (RuntimeException ex) {
            LOGGER.info("Long lasting task execution failed. Cause: ", ex);
            response = new HelloWorldLongLastingTaskFailureResponse(resourceId, ex);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}

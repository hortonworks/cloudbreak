package com.sequenceiq.flow.core.helloworld.reactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFirstStepLongLastingTaskFailureResponse;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFirstStepLongLastingTaskSuccessResponse;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFirstStepLongLastingTaskTriggerEvent;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class HelloWorldFirstStepLongLastingTaskHandler extends ExceptionCatcherEventHandler<HelloWorldFirstStepLongLastingTaskTriggerEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldFirstStepLongLastingTaskHandler.class);

    @Override
    public String selector() {
        return HelloWorldFirstStepLongLastingTaskTriggerEvent.class.getSimpleName();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<HelloWorldFirstStepLongLastingTaskTriggerEvent> event) {
        return new HelloWorldFirstStepLongLastingTaskFailureResponse(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<HelloWorldFirstStepLongLastingTaskTriggerEvent> event) {
        HelloWorldFirstStepLongLastingTaskTriggerEvent helloWorldReactorEvent = event.getData();
        Long resourceId = helloWorldReactorEvent.getResourceId();
        try {
            LOGGER.info("Long lasting task execution...");
            return new HelloWorldFirstStepLongLastingTaskSuccessResponse(resourceId);
        } catch (RuntimeException ex) {
            LOGGER.info("Long lasting task execution failed. Cause: ", ex);
            return new HelloWorldFirstStepLongLastingTaskFailureResponse(resourceId, ex);
        }
    }
}

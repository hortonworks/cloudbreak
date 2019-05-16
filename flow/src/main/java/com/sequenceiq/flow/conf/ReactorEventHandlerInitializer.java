package com.sequenceiq.flow.conf;

import static reactor.bus.selector.Selectors.$;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.EventBus;

@Configuration
public class ReactorEventHandlerInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorEventHandlerInitializer.class);

    private final EventHandlerConfiguration.EventHandlers eventHandlers;

    private final EventBus eventBus;

    public ReactorEventHandlerInitializer(EventHandlerConfiguration.EventHandlers eventHandlers, EventBus eventBus) {
        this.eventHandlers = eventHandlers;
        this.eventBus = eventBus;

        validateSelectors();
        LOGGER.debug("Registering ReactorEventHandlers");
        for (EventHandler<?> handler : eventHandlers.getEventHandlers()) {
            String selector = handler.selector();
            LOGGER.debug("Registering handler [{}] for selector [{}]", handler.getClass(), selector);
            eventBus.on($(selector), handler);
        }
    }

    private void validateSelectors() {
        LOGGER.debug("There are {} handlers suitable for registering", eventHandlers.getEventHandlers().size());
        Map<String, EventHandler<?>> handlerMap = new HashMap<>();
        for (EventHandler<?> handler : eventHandlers.getEventHandlers()) {
            EventHandler<?> entry = handlerMap.put(handler.selector(), handler);
            if (null != entry) {
                LOGGER.error("Duplicated handlers! actual: {}, existing: {}", handler, entry);
                throw new IllegalStateException("Duplicate handlers! first: " + handler + " second: " + entry);
            }
        }
    }
}

package com.sequenceiq.flow.handler.init;

import static reactor.bus.selector.Selectors.$;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.handler.EventHandler;

import reactor.bus.EventBus;

@Component
public class ReactorEventHandlerInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorEventHandlerInitializer.class);

    @Resource
    private List<EventHandler> handlers = new ArrayList<>();

    @Inject
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        validateSelectors();
        LOGGER.debug("Registering ReactorEventHandlers");
        for (EventHandler handler : handlers) {
            String selector = handler.selector();
            LOGGER.debug("Registering handler [{}] for selector [{}]", handler.getClass(), selector);
            eventBus.on($(selector), handler);
        }
    }

    private void validateSelectors() {
        LOGGER.debug("There are {} handlers suitable for registering", handlers.size());
        Map<String, EventHandler> handlerMap = new HashMap<>();
        for (EventHandler handler : handlers) {
            EventHandler entry = handlerMap.put(handler.selector(), handler);
            if (null != entry) {
                LOGGER.error("Duplicated handlers! actual: {}, existing: {}", handler, entry);
                throw new IllegalStateException("Duplicate handlers! first: " + handler + " second: " + entry);
            }
        }
    }
}

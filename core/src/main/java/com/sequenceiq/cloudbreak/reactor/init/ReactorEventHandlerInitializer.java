package com.sequenceiq.cloudbreak.reactor.init;

import static reactor.bus.selector.Selectors.$;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.EventBus;

@Component
public class ReactorEventHandlerInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorEventHandlerInitializer.class);

    @Resource
    private List<ReactorEventHandler> handlers;

    @Inject
    private EventBus eventBus;

    @PostConstruct
    public void init() throws Exception {
        validateSelectors();
        LOGGER.info("Registering ReactorEventHandlers");
        for (ReactorEventHandler handler : handlers) {
            String selector = handler.selector();
            LOGGER.info("Registering handler [{}] for selector [{}]", handler.getClass(), selector);
            eventBus.on($(selector), handler);
        }
    }

    private void validateSelectors() {
        LOGGER.debug("There are {} handlers suitable for registering", handlers.size());
        Map<String, ReactorEventHandler> handlerMap = new HashMap<>();
        for (ReactorEventHandler handler : handlers) {
            ReactorEventHandler entry = handlerMap.put(handler.selector(), handler);
            if (null != entry) {
                LOGGER.error("Duplicated handlers! actual: {}, existing: {}", handler, entry);
                throw new IllegalStateException("Duplicate handlers! first: " + handler + " second: " + entry);
            }
        }
    }
}

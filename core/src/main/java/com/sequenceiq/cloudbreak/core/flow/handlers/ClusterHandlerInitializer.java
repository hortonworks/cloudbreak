package com.sequenceiq.cloudbreak.core.flow.handlers;

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

import reactor.bus.EventBus;

@Component
public class ClusterHandlerInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterHandlerInitializer.class);

    @Resource
    private List<AmbariClusterEventHandler> handlers;

    @Inject
    private EventBus eventBus;

    @PostConstruct
    public void init() throws Exception {
        validateSelectors();
        LOGGER.info("Registering AmbariClusterEventHandlers");
        for (AmbariClusterEventHandler handler : handlers) {
            String selector = AmbariClusterRequest.selector(handler.type());
            LOGGER.info("Registering handler [{}] for selector [{}]", handler.getClass(), selector);
            eventBus.on($(selector), handler);
        }
    }

    private void validateSelectors() {
        LOGGER.debug("There are {} handlers suitable for registering", handlers.size());
        Map<Class, AmbariClusterEventHandler> handlerMap = new HashMap<>();
        for (AmbariClusterEventHandler handler : handlers) {
            AmbariClusterEventHandler entry = handlerMap.put(handler.type(), handler);
            if (null != entry) {
                LOGGER.error("Duplicate handlers! actual: {}, existing: {}", handler, entry);
                throw new IllegalStateException("Duplicate handlers! first: " + handler + " second: " + entry);
            }
        }
    }
}

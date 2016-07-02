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

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;

import reactor.bus.EventBus;

@Component
public class ClusterPlatformInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPlatformInitializer.class);

    @Resource
    private List<ClusterEventHandler> handlers;

    @Inject
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        validateSelectors();
        LOGGER.info("Registering ClusterEventHandler");
        for (ClusterEventHandler handler : handlers) {
            String selector = EventSelectorUtil.selector(handler.type());
            LOGGER.info("Registering handler [{}] for selector [{}]", handler.getClass(), selector);
            eventBus.on($(selector), handler);
        }
    }

    private void validateSelectors() {
        LOGGER.debug("There are {} handlers suitable for registering", handlers.size());
        Map<Class, ClusterEventHandler> handlerMap = new HashMap<>();
        for (ClusterEventHandler handler : handlers) {
            ClusterEventHandler entry = handlerMap.put(handler.type(), handler);
            if (null != entry) {
                LOGGER.error("Duplicated handlers! actual: {}, existing: {}", handler, entry);
                throw new IllegalStateException("Duplicate handlers! first: " + handler + " second: " + entry);
            }
        }
    }
}

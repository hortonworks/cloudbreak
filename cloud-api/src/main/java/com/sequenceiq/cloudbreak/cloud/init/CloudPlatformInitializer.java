package com.sequenceiq.cloudbreak.cloud.init;

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

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;

import reactor.bus.EventBus;

@Component
public class CloudPlatformInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudPlatformInitializer.class);

    @Resource
    private List<CloudPlatformEventHandler> handlers;

    @Inject
    private EventBus eventBus;

    @PostConstruct
    public void init() throws Exception {
        LOGGER.info("Registering CloudPlatformEventHandlers");
        Map<Class, CloudPlatformEventHandler> handlersMap = handlersMap();
        register(LaunchStackRequest.class, handlersMap);
    }

    public Map<Class, CloudPlatformEventHandler> handlersMap() {
        Map<Class, CloudPlatformEventHandler> handlersMap = new HashMap<>();
        for (CloudPlatformEventHandler handler : handlers) {
            CloudPlatformEventHandler existing = handlersMap.put(handler.type(), handler);
            if (existing != null) {
                throw new IllegalStateException("Duplicate Handler registration: " + existing.getClass() + " and " + handler.getClass());
            }
        }
        return handlersMap;
    }

    private void register(Class eventClass, Map<Class, CloudPlatformEventHandler> handlersMap) {
        CloudPlatformEventHandler handler = get(handlersMap, eventClass);
        String selector = CloudPlatformRequest.selector(handler.type());
        LOGGER.info("Registering CloudPlatformEventHandler: selector: {}, type: {}, class: {}", selector, handler
                        .type().getSimpleName(),
                handler.getClass());
        eventBus.on($(selector), handler);
    }

    private CloudPlatformEventHandler get(Map<Class, CloudPlatformEventHandler> handlersMap, Class eventClass) {
        CloudPlatformEventHandler handler = handlersMap.get(eventClass);
        if (handler == null) {
            throw new IllegalStateException("No registered handler found for " + eventClass + " Check your configuration");
        }
        return handler;
    }


}

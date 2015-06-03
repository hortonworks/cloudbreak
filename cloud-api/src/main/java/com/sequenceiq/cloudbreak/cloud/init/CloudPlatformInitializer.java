package com.sequenceiq.cloudbreak.cloud.init;

import static reactor.bus.selector.Selectors.$;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
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
        for (CloudPlatformEventHandler handler : handlers) {
            String selector = CloudPlatformRequest.selector(handler.type());
            LOGGER.info("Registering handler [{}] for selector [{}]", handler.getClass(), selector);
            eventBus.on($(selector), handler);
        }
    }
}

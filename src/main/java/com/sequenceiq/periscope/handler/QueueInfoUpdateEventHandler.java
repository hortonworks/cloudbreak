package com.sequenceiq.periscope.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.event.QueueInfoUpdateEvent;

@Component
public class QueueInfoUpdateEventHandler implements ApplicationListener<QueueInfoUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueInfoUpdateEventHandler.class);

    @Override
    public void onApplicationEvent(QueueInfoUpdateEvent event) {
        LOGGER.info("We should do something with this event.. {}", event.getClusterId());
    }
}

package com.sequenceiq.periscope.monitor.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.QueueInfoUpdateFailedEvent;

@Component
public class QueueInfoUpdateFailedEventHandler implements ApplicationListener<QueueInfoUpdateFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueInfoUpdateFailedEventHandler.class);

    @Override
    public void onApplicationEvent(QueueInfoUpdateFailedEvent event) {
        LOGGER.info("Queue metrics update failed for cluster {}", event.getClusterId());
    }
}

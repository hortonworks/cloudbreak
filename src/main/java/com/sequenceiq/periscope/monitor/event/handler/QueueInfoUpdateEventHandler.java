package com.sequenceiq.periscope.monitor.event.handler;

import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.QueueInfoUpdateEvent;

@Component
public class QueueInfoUpdateEventHandler implements ApplicationListener<QueueInfoUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueInfoUpdateEventHandler.class);

    @Override
    public void onApplicationEvent(QueueInfoUpdateEvent event) {
        for (QueueInfo info : event.getQueueInfo()) {
            printQueueMetrics(info);
        }
    }

    private void printQueueMetrics(QueueInfo info) {
        StringBuilder sb = new StringBuilder();

        sb.append("\nQueue name: ").append(info.getQueueName());
        sb.append("\ncapacity: ").append(info.getCapacity());
        sb.append("\nmax capacity: ").append(info.getMaximumCapacity());
        sb.append("\ncurrent consumption: ").append(info.getCurrentCapacity());

        LOGGER.info(sb.toString());
    }
}
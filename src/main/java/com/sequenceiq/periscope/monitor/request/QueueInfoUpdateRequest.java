package com.sequenceiq.periscope.monitor.request;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.monitor.event.QueueInfoUpdateEvent;
import com.sequenceiq.periscope.registry.ClusterRegistration;

@Component
@Scope("prototype")
public class QueueInfoUpdateRequest extends AbstractEventPublisher implements Runnable, EventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueInfoUpdateRequest.class);

    private ClusterRegistration clusterRegistration;

    public QueueInfoUpdateRequest(ClusterRegistration clusterRegistration) {
        this.clusterRegistration = clusterRegistration;
    }

    @Override
    public void run() {
        try {
            List<QueueInfo> queues = clusterRegistration.getYarnClient().getAllQueues();
            publishEvent(new QueueInfoUpdateEvent(clusterRegistration.getClusterId(), queues));
        } catch (IOException | YarnException e) {
            LOGGER.error("Error occurred during scheduler metrics update", e);
        }
    }

}

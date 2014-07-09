package com.sequenceiq.periscope.monitor;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.event.QueueInfoUpdateEvent;
import com.sequenceiq.periscope.registry.ClusterRegistration;
import com.sequenceiq.periscope.registry.ClusterRegistry;

@Component
@Qualifier("queue")
public class QueueMonitor extends AbstractMonitor implements Monitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueMonitor.class);

    @Autowired
    private ClusterRegistry clusterRegistry;

    @Override
    public void update() {
        for (ClusterRegistration clusterRegistration : clusterRegistry.getAll()) {
            try {
                List<QueueInfo> queues = clusterRegistration.getYarnClient().getAllQueues();
                publishEvent(new QueueInfoUpdateEvent(clusterRegistration.getClusterId(), queues));
            } catch (IOException | YarnException e) {
                LOGGER.error("Error occurred during scheduler metrics update", e);
            }
        }
    }

}

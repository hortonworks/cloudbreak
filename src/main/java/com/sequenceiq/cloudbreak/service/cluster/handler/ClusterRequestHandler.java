package com.sequenceiq.cloudbreak.service.cluster.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class ClusterRequestHandler implements Consumer<Event<Stack>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRequestHandler.class);

    @Autowired
    private AmbariClusterConnector ambariClusterInstaller;

    @Override
    public void accept(Event<Stack> event) {
        String eventKey = (String) event.getKey();
        Stack stack = event.getData();
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", eventKey);
        if (ReactorConfig.AMBARI_STARTED_EVENT.equals(eventKey)) {
            if (stack.getCluster() != null && stack.getCluster().getStatus().equals(Status.REQUESTED)) {
                ambariClusterInstaller.installAmbariCluster(stack);
            } else {
                LOGGER.info("Ambari has started but there were no cluster request to this stack yet. Won't install cluster now.");
            }
        } else if (ReactorConfig.CLUSTER_REQUESTED_EVENT.equals(eventKey)) {
            if (stack.getStatus().equals(Status.AVAILABLE)) {
                ambariClusterInstaller.installAmbariCluster(stack);
            } else {
                LOGGER.info("Cluster install requested but the stack is not completed yet. Installation will start after the stack is ready.");
            }
        }
    }
}

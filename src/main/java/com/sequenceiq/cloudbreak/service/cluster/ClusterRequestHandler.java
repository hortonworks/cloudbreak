package com.sequenceiq.cloudbreak.service.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class ClusterRequestHandler implements Consumer<Event<Stack>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRequestHandler.class);

    @Autowired
    private AmbariClusterInstaller ambariClusterInstaller;

    @Override
    public void accept(Event<Stack> event) {
        String eventKey = (String) event.getKey();
        Stack stack = event.getData();
        LOGGER.info("Accepted {} event.", eventKey);
        if (ReactorConfig.AMBARI_STARTED_EVENT.equals(eventKey)) {
            if (stack.getCluster() != null && stack.getCluster().getStatus().equals(Status.REQUESTED)) {
                ambariClusterInstaller.installAmbariCluster(stack);
            } else {
                LOGGER.info("Ambari has started but there were no cluster request to this stack yet. Won't install cluster now. [stack: {}]", stack.getId());
            }
        } else if (ReactorConfig.CLUSTER_REQUESTED_EVENT.equals(eventKey)) {
            if (stack.getStatus().equals(Status.CREATE_COMPLETED)) {
                ambariClusterInstaller.installAmbariCluster(stack);
            } else {
                LOGGER.info("Cluster install requested but the stack is not completed yet. Installation will start after the stack is ready. [stack: {}]",
                        stack.getId());
            }
        }
    }
}

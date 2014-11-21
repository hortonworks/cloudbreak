package com.sequenceiq.cloudbreak.service.cluster.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class UpdateAmbariHostsRequestHandler implements Consumer<Event<UpdateAmbariHostsRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAmbariHostsRequestHandler.class);

    @Autowired
    private AmbariClusterConnector ambariClusterConnector;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public void accept(Event<UpdateAmbariHostsRequest> event) {
        UpdateAmbariHostsRequest request = event.getData();
        Stack stack = stackRepository.findById(request.getStackId());
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", ReactorConfig.UPDATE_AMBARI_HOSTS_REQUEST_EVENT);
        HostGroupAdjustmentJson adjustment = request.getHostGroupAdjustment();
        if (request.isDecommission()) {
            ambariClusterConnector.decommissionAmbariNodes(request.getStackId(), adjustment.getScalingAdjustment(), request.getDecommissionCandidates());
        } else {
            ambariClusterConnector.installAmbariNode(request.getStackId(), adjustment);
        }
    }
}

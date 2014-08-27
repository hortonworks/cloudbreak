package com.sequenceiq.cloudbreak.service.cluster.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;

@Component
public class UpdateAmbariHostsRequestHandler implements Consumer<Event<UpdateAmbariHostsRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAmbariHostsRequestHandler.class);

    @Autowired
    private AmbariClusterConnector ambariClusterConnector;

    @Override
    public void accept(Event<UpdateAmbariHostsRequest> event) {
        UpdateAmbariHostsRequest request = event.getData();
        LOGGER.info("Accepted {} event.", ReactorConfig.UPDATE_AMBARI_HOSTS_REQUEST_EVENT, request.getStackId());
        if (request.isDecommision()) {
            ambariClusterConnector.decommisionAmbariNodes(request.getStackId(), request.getHosts());
        } else {
            ambariClusterConnector.installAmbariNode(request.getStackId(), request.getHosts());
        }
    }
}

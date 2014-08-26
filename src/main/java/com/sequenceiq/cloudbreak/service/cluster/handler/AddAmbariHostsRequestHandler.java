package com.sequenceiq.cloudbreak.service.cluster.handler;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.cluster.event.AddAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterInstaller;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;

@Component
public class AddAmbariHostsRequestHandler implements Consumer<Event<AddAmbariHostsRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddAmbariHostsRequestHandler.class);

    @Autowired
    private AmbariClusterInstaller ambariClusterInstaller;

    @Resource
    private Map<CloudPlatform, Provisioner> provisioners;

    @Override
    public void accept(Event<AddAmbariHostsRequest> event) {
        AddAmbariHostsRequest data = event.getData();
        LOGGER.info("Accepted {} event.", ReactorConfig.ADD_AMBARI_HOSTS_REQUEST_EVENT, data.getStackId());
        ambariClusterInstaller.installAmbariNode(data.getStackId(), data.getHosts());
    }
}

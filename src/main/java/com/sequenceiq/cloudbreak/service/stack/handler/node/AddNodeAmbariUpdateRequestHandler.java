package com.sequenceiq.cloudbreak.service.stack.handler.node;

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
import com.sequenceiq.cloudbreak.service.cluster.AmbariClusterInstaller;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.event.AmbariAddNode;

@Component
public class AddNodeAmbariUpdateRequestHandler implements Consumer<Event<AmbariAddNode>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddNodeAmbariUpdateRequestHandler.class);

    @Autowired
    private AmbariClusterInstaller ambariClusterInstaller;

    @Resource
    private Map<CloudPlatform, Provisioner> provisioners;

    @Override
    public void accept(Event<AmbariAddNode> event) {
        AmbariAddNode data = event.getData();
        LOGGER.info("Accepted {} event.", ReactorConfig.ADD_NODE_AMBARI_UPDATE_NODE_EVENT, data.getStackId());
        ambariClusterInstaller.installAmbariNode(data.getStackId(), data.getHosts());
    }
}

package com.sequenceiq.cloudbreak.service.stack.handler.node;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.service.stack.event.AddNodeMetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.domain.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariRoleAllocator;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class AddNodeMetadataSetupCompleteHandler implements Consumer<Event<AddNodeMetadataSetupComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddNodeMetadataSetupCompleteHandler.class);

    @Autowired
    private AmbariRoleAllocator ambariRoleAllocator;

    @Override
    public void accept(Event<AddNodeMetadataSetupComplete> event) {
        AddNodeMetadataSetupComplete data = event.getData();
        Long stackId = data.getStackId();
        Set<CoreInstanceMetaData> coreInstanceMetaData = data.getCoreInstanceMetaData();
        LOGGER.info("Accepted {} event.", ReactorConfig.ADD_NODE_UPDATE_METADATA_EVENT_COMPLETE, stackId);
        ambariRoleAllocator.updateInstanceMetadata(stackId, coreInstanceMetaData);
    }
}

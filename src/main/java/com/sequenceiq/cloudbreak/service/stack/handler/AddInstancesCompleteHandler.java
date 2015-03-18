package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.service.stack.event.AddInstancesComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupContext;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class AddInstancesCompleteHandler implements Consumer<Event<AddInstancesComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddInstancesCompleteHandler.class);

    @Autowired private MetadataSetupContext metadataSetupContext;

    @Override
    public void accept(Event<AddInstancesComplete> event) {
        AddInstancesComplete data = event.getData();
        LOGGER.info("Accepted {} event.", ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT);

        metadataSetupContext.updateMetadata(data.getCloudPlatform(), data.getStackId(), data.getResources(), data.getInstanceGroup());
    }
}

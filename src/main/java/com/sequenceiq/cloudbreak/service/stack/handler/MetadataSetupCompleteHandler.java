package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.domain.CoreInstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariRoleAllocator;

@Component
public class MetadataSetupCompleteHandler implements Consumer<Event<MetadataSetupComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupCompleteHandler.class);

    @Autowired
    private AmbariRoleAllocator ambariRoleAllocator;

    @Override
    public void accept(Event<MetadataSetupComplete> event) {
        MetadataSetupComplete metadataSetupComplete = event.getData();
        Long stackId = metadataSetupComplete.getStackId();
        Set<CoreInstanceMetaData> coreInstanceMetaData = metadataSetupComplete.getCoreInstanceMetaData();
        LOGGER.info("Accepted {} event.", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stackId);
        ambariRoleAllocator.allocateRoles(stackId, coreInstanceMetaData);
    }
}

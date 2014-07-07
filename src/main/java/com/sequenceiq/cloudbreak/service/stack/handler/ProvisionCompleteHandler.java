package com.sequenceiq.cloudbreak.service.stack.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupContext;

@Component
public class ProvisionCompleteHandler implements Consumer<Event<ProvisionComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionCompleteHandler.class);

    @Autowired
    private MetadataSetupContext metadataSetupContext;

    @Override
    public void accept(Event<ProvisionComplete> event) {
        ProvisionComplete stackCreateComplete = event.getData();
        CloudPlatform cloudPlatform = stackCreateComplete.getCloudPlatform();
        Long stackId = stackCreateComplete.getStackId();
        LOGGER.info("Accepted {} event.", ReactorConfig.PROVISION_COMPLETE_EVENT, stackId);
        metadataSetupContext.setupMetadata(cloudPlatform, stackId);
    }

}

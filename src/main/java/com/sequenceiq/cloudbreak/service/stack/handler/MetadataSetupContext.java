package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.StackCreationFailure;
import com.sequenceiq.cloudbreak.service.stack.event.StackCreateComplete;

@Component
public class MetadataSetupContext implements Consumer<Event<StackCreateComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupContext.class);

    @Autowired
    private StackRepository stackRepository;

    @Resource
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    @Autowired
    private Reactor reactor;

    @Override
    public void accept(Event<StackCreateComplete> event) {
        StackCreateComplete stackCreateComplete = event.getData();
        CloudPlatform cloudPlatform = stackCreateComplete.getCloudPlatform();
        Long stackId = stackCreateComplete.getStackId();
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_CREATE_COMPLETE_EVENT, stackId);
        try {
            Stack stack = stackRepository.findById(stackId);
            MetadataSetup metadataSetup = metadataSetups.get(cloudPlatform);
            metadataSetup.setupMetadata(stack);
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while creating stack.", e);
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
            StackCreationFailure stackCreationFailure = new StackCreationFailure(stackId, "Internal server error occured while creating stack.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        }

    }

}

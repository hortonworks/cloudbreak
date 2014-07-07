package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.Reactor;
import reactor.event.Event;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.event.StackCreationFailure;

@Service
public class MetadataSetupContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupContext.class);

    @Autowired
    private StackRepository stackRepository;

    @Resource
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    @Autowired
    private Reactor reactor;

    public void setupMetadata(CloudPlatform cloudPlatform, Long stackId) {
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

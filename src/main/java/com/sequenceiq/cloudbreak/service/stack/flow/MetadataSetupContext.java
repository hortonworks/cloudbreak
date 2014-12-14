package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class MetadataSetupContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupContext.class);

    @Autowired
    private StackRepository stackRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    @Autowired
    private Reactor reactor;

    public void setupMetadata(CloudPlatform cloudPlatform, Long stackId) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        try {
            MetadataSetup metadataSetup = metadataSetups.get(cloudPlatform);
            metadataSetup.setupMetadata(stack);
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occurred while creating stack.", e);
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_CREATE_FAILED_EVENT, stackId);
            StackOperationFailure stackCreationFailure = new StackOperationFailure(stackId, "Internal server error occurred while creating stack.");
            reactor.notify(ReactorConfig.STACK_CREATE_FAILED_EVENT, Event.wrap(stackCreationFailure));
        }
    }

    public void updateMetadata(CloudPlatform cloudPlatform, Long stackId, Set<Resource> resourceSet, String hostGroup) {
        Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        try {
            metadataSetups.get(cloudPlatform).addNewNodesToMetadata(stack, resourceSet, hostGroup);
        } catch (Exception e) {
            String errMessage = "Unhandled exception occurred while updating stack metadata.";
            LOGGER.error(errMessage, e);
            LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.STACK_UPDATE_FAILED_EVENT, stackId);
            reactor.notify(ReactorConfig.STACK_UPDATE_FAILED_EVENT, Event.wrap(new StackOperationFailure(stackId, errMessage)));
        }

    }

}

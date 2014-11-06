package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariRoleAllocator;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class MetadataSetupCompleteHandler implements Consumer<Event<MetadataSetupComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupCompleteHandler.class);

    @Autowired
    private AmbariRoleAllocator ambariRoleAllocator;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public void accept(Event<MetadataSetupComplete> event) {
        MetadataSetupComplete metadataSetupComplete = event.getData();
        Long stackId = metadataSetupComplete.getStackId();
        Stack stack = stackRepository.findById(stackId);
        MDCBuilder.buildMdcContext(stack);
        Set<CoreInstanceMetaData> coreInstanceMetaData = metadataSetupComplete.getCoreInstanceMetaData();
        LOGGER.info("Accepted {} event.", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stackId);
        ambariRoleAllocator.allocateRoles(stackId, coreInstanceMetaData);
    }
}

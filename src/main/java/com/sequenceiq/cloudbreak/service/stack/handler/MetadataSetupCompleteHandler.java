package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;
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
        MDC.put(LoggerContextKey.OWNER_ID.toString(), stack.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), stack.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.STACK_ID.toString());
        Set<CoreInstanceMetaData> coreInstanceMetaData = metadataSetupComplete.getCoreInstanceMetaData();
        LOGGER.info("Accepted {} event.", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stackId);
        ambariRoleAllocator.allocateRoles(stackId, coreInstanceMetaData);
    }
}

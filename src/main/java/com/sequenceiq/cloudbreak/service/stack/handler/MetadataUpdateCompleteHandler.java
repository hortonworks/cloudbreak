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
import com.sequenceiq.cloudbreak.service.stack.event.MetadataUpdateComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariRoleAllocator;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class MetadataUpdateCompleteHandler implements Consumer<Event<MetadataUpdateComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataUpdateCompleteHandler.class);

    @Autowired
    private AmbariRoleAllocator ambariRoleAllocator;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public void accept(Event<MetadataUpdateComplete> event) {
        MetadataUpdateComplete data = event.getData();
        Long stackId = data.getStackId();
        Set<CoreInstanceMetaData> coreInstanceMetaData = data.getCoreInstanceMetaData();
        Stack stack = stackRepository.findById(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", ReactorConfig.METADATA_UPDATE_COMPLETE_EVENT, stackId);
        ambariRoleAllocator.updateInstanceMetadata(stackId, coreInstanceMetaData, data.getHostGroup());
    }
}

package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.event.AddInstancesComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupContext;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class AddInstancesCompleteHandler implements Consumer<Event<AddInstancesComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddInstancesCompleteHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private MetadataSetupContext metadataSetupContext;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Override
    public void accept(Event<AddInstancesComplete> event) {
        AddInstancesComplete data = event.getData();
        CloudPlatform cloudPlatform = data.getCloudPlatform();
        Long stackId = data.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Set<Resource> resourcesSet = event.getData().getResources();

        MDC.put(LoggerContextKey.OWNER_ID.toString(), stack.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), stack.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.STACK_ID.toString());
        LOGGER.info("Accepted {} event on stack '{}'.", ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT, stackId);
        if (resourcesSet != null) {
            Set<Resource> resources = stack.getResources();
            resources.addAll(resourcesSet);
            retryingStackUpdater.updateStackResources(stackId, resources);
        }
        metadataSetupContext.updateMetadata(cloudPlatform, stackId, resourcesSet);
    }
}

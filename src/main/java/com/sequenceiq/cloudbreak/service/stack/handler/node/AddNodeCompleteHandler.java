package com.sequenceiq.cloudbreak.service.stack.handler.node;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.event.AddNodeComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupContext;

@Component
public class AddNodeCompleteHandler implements Consumer<Event<AddNodeComplete>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddNodeCompleteHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private MetadataSetupContext metadataSetupContext;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Override
    public void accept(Event<AddNodeComplete> event) {
        AddNodeComplete data = event.getData();
        CloudPlatform cloudPlatform = data.getCloudPlatform();
        Long stackId = data.getStackId();
        Set<Resource> resourcesSet = event.getData().getResources();
        LOGGER.info("Accepted {} event on stack '{}'.", ReactorConfig.ADD_NODE_COMPLETE_EVENT, stackId);
        if (resourcesSet != null) {
            Stack stack = stackRepository.findOneWithLists(stackId);
            Set<Resource> resources = stack.getResources();
            resources.addAll(resourcesSet);
            retryingStackUpdater.updateStackResources(stackId, resources);
        }
        metadataSetupContext.setupHostMetadata(cloudPlatform, stackId, resourcesSet);
    }
}

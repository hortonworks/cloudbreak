package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class StackUpdateSuccessHandler implements Consumer<Event<StackUpdateSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpdateSuccessHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Override
    public void accept(Event<StackUpdateSuccess> t) {
        StackUpdateSuccess updateSuccess = t.getData();
        Long stackId = updateSuccess.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        MDC.put(LoggerContextKey.OWNER_ID.toString(), stack.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), stack.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.STACK_ID.toString());
        LOGGER.info("Accepted {} event.", ReactorConfig.STACK_UPDATE_SUCCESS_EVENT, stackId);
        Set<String> instanceIds = updateSuccess.getInstanceIds();
        if (updateSuccess.isRemoveInstances()) {
            stackUpdater.updateNodeCount(stackId, stack.getNodeCount() - instanceIds.size());
            Set<InstanceMetaData> metadataToRemove = new HashSet<>();
            for (InstanceMetaData metadataEntry : stack.getInstanceMetaData()) {
                for (String instanceId : instanceIds) {
                    if (metadataEntry.getInstanceId().equals(instanceId)) {
                        metadataToRemove.add(metadataEntry);
                    }
                }
            }
            stack.getInstanceMetaData().removeAll(metadataToRemove);
            stackUpdater.updateStackMetaData(stackId, stack.getInstanceMetaData());
            LOGGER.info("Successfully removed metadata of instances '{}' in stack '{}'", instanceIds, stackId);
        } else {
            stackUpdater.updateNodeCount(stackId, stack.getNodeCount() + instanceIds.size());
        }
        stackUpdater.updateMetadataReady(stackId, true);
        stackUpdater.updateStackStatus(stackId, Status.AVAILABLE);

    }

}

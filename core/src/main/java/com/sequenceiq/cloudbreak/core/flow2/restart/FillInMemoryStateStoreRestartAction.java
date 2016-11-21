package com.sequenceiq.cloudbreak.core.flow2.restart;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("FillInMemoryStateStoreRestartAction")
public class FillInMemoryStateStoreRestartAction extends DefaultRestartAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FillInMemoryStateStoreRestartAction.class);

    @Inject
    private StackService stackService;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Stack stack = null;
        try {
            Payload stackPayload = (Payload) payload;
            stack = stackService.getById(stackPayload.getStackId());
            InMemoryStateStore.putStack(stack.getId(), statusToPollGroupConverter.convert(stack.getStatus()));
            InMemoryStateStore.putCluster(stack.getCluster().getId(), statusToPollGroupConverter.convert(stack.getCluster().getStatus()));
        } catch (Exception e) {
            if (stack != null) {
                MDCBuilder.buildMdcContext(stack);
            }
            LOGGER.error("Failed to restore stack into InMemoryStateStore", e);
        }
        super.restart(flowId, flowChainId, event, payload);
    }
}

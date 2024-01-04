package com.sequenceiq.cloudbreak.core.flow2.restart;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.RestartContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("FillInMemoryStateStoreRestartAction")
public class FillInMemoryStateStoreRestartAction extends DefaultRestartAction {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    @Override
    public void doBeforeRestart(RestartContext restartContext, Object payload) {
        StackView stack = stackDtoService.getStackViewById(restartContext.getResourceId());
        InMemoryStateStore.putStack(stack.getId(), statusToPollGroupConverter.convert(stack.getStatus()));
        if (stack.getClusterId() != null) {
            InMemoryStateStore.putCluster(stack.getClusterId(), statusToPollGroupConverter.convert(stack.getStatus()));
        }
        MDCBuilder.buildMdcContext(stack);
    }
}

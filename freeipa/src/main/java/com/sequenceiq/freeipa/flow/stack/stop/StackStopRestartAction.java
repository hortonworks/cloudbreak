package com.sequenceiq.freeipa.flow.stack.stop;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.RestartContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Component("StackStopRestartAction")
public class StackStopRestartAction extends DefaultRestartAction {

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    public void doBeforeRestart(RestartContext restartContext, Object payload) {
        Stack stack = stackService.getStackById(restartContext.getResourceId());
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.STOP_REQUESTED, "Stop/restart");
        MDCBuilder.buildMdcContext(stack);
    }
}
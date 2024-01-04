package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.RestartContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("StackStopRestartAction")
public class StackStopRestartAction extends DefaultRestartAction {

    @Inject
    private StackService stackService;

    @Override
    public void doBeforeRestart(RestartContext restartContext, Object payload) {
        Stack stack = stackService.getById(restartContext.getResourceId());
        MDCBuilder.buildMdcContext(stack);
    }
}

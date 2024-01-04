package com.sequenceiq.freeipa.flow;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.RestartContext;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component("InitializeMDCContextRestartAction")
public class InitializeMDCContextRestartAction extends DefaultRestartAction {

    @Inject
    private StackService stackService;

    @Override
    public void doBeforeRestart(RestartContext restartContext, Object payload) {
        Stack stack = stackService.getStackById(restartContext.getResourceId());
        MDCBuilder.buildMdcContext(stack);
    }
}

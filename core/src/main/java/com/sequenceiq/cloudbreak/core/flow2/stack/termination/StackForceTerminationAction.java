package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;

@Component("StackForceTerminationAction")
public class StackForceTerminationAction extends StackTerminationAction {
    @Override
    protected Long getStackId(DefaultFlowContext payload) {
        return payload.getStackId();
    }

    @Override
    protected void doExecute(StackTerminationContext context, DefaultFlowContext payload, Map<Object, Object> variables) {
        variables.put("FORCEDTERMINATION", Boolean.TRUE);
        doExecute(context);
    }
}

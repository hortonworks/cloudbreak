package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
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
        TerminateStackRequest<TerminateStackResult> terminateRequest = new TerminateStackRequest<>(context.getCloudContext(), context.getCloudStack(),
                context.getCloudCredential(), context.getCloudResources());
        doExecute(context, terminateRequest);
    }
}

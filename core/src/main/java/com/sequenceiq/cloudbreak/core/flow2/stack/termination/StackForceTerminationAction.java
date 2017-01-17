package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationEvent;

@Component("StackForceTerminationAction")
public class StackForceTerminationAction extends StackTerminationAction {
    @Override
    protected void prepareExecution(TerminationEvent payload, Map<Object, Object> variables) {
        variables.put("FORCEDTERMINATION", Boolean.TRUE);
        variables.put("DELETEDEPENDENCIES", payload.getDeleteDependencies());
    }
}

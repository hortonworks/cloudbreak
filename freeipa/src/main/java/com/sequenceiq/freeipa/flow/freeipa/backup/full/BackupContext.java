package com.sequenceiq.freeipa.flow.freeipa.backup.full;

import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.Stack;

public class BackupContext extends CommonContext {

    private final Stack stack;

    public BackupContext(FlowParameters flowParameters, Stack stack) {
        super(flowParameters);
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }
}

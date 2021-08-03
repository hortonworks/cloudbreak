package com.sequenceiq.freeipa.flow.freeipa.backup.full.event;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CreateFullBackupEvent extends StackEvent {
    public CreateFullBackupEvent(Long stackId) {
        super(stackId);
    }
}

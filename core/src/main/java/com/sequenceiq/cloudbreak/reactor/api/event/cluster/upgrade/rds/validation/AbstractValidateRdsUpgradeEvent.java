package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public abstract class AbstractValidateRdsUpgradeEvent extends StackEvent {

    public AbstractValidateRdsUpgradeEvent(Long stackId) {
        super(stackId);
    }

    public AbstractValidateRdsUpgradeEvent(String selector, Long stackId) {
        super(selector, stackId);
    }

    public AbstractValidateRdsUpgradeEvent(String selector, Long stackId, Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }
}
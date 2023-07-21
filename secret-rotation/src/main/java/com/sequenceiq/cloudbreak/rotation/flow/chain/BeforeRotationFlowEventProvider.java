package com.sequenceiq.cloudbreak.rotation.flow.chain;

import com.sequenceiq.cloudbreak.common.event.Selectable;

public interface BeforeRotationFlowEventProvider {

    Selectable getTriggerEvent(SecretRotationFlowChainTriggerEvent event);
}

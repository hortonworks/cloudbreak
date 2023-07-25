package com.sequenceiq.datalake.service.rotation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.flow.chain.BeforeRotationFlowEventProvider;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.datalake.flow.salt.update.SaltUpdateEvent;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateTriggerEvent;

@Component
public class DatalakeSaltUpdateBeforeRotationEventProvider implements BeforeRotationFlowEventProvider {

    @Override
    public Selectable getTriggerEvent(SecretRotationFlowChainTriggerEvent event) {
        return new SaltUpdateTriggerEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(),
                ThreadBasedUserCrnProvider.getUserCrn(), event.accepted());
    }
}

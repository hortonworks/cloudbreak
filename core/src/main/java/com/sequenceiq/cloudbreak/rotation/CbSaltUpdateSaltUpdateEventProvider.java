package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent.SALT_UPDATE_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SaltUpdateFlowEventProvider;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;

@Component
public class CbSaltUpdateSaltUpdateEventProvider implements SaltUpdateFlowEventProvider {

    @Override
    public Selectable getSaltUpdateTriggerEvent(SecretRotationFlowChainTriggerEvent event) {
        return new StackEvent(SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted());
    }
}

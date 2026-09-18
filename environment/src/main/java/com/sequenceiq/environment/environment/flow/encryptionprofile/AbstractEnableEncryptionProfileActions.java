package com.sequenceiq.environment.environment.flow.encryptionprofile;

import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

abstract class AbstractEnableEncryptionProfileActions<P extends BaseNamedFlowEvent>
        extends AbstractEncryptionProfileAction<EnabledEncryptionProfileState, EnableEncryptionProfileStateSelectors, P> {

    protected AbstractEnableEncryptionProfileActions(Class<P> payloadClass) {
        super(payloadClass, EnableEncryptionProfileFailedEvent::from);
    }
}

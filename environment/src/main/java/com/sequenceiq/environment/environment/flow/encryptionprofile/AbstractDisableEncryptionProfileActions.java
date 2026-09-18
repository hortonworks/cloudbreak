package com.sequenceiq.environment.environment.flow.encryptionprofile;

import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

abstract class AbstractDisableEncryptionProfileActions<P extends BaseNamedFlowEvent>
        extends AbstractEncryptionProfileAction<DisableEncryptionProfileState, DisableEncryptionProfileStateSelectors, P> {

    protected AbstractDisableEncryptionProfileActions(Class<P> payloadClass) {
        super(payloadClass, DisableEncryptionProfileFailedEvent::from);
    }
}

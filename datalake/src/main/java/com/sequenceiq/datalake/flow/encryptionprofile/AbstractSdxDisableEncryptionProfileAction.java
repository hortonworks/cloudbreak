package com.sequenceiq.datalake.flow.encryptionprofile;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxDisableEncryptionProfileFailedEvent;

abstract class AbstractSdxDisableEncryptionProfileAction<P extends SdxEvent> extends AbstractSdxEncryptionProfileAction<P> {

    protected AbstractSdxDisableEncryptionProfileAction(Class<P> payloadClass) {
        super(payloadClass, SdxDisableEncryptionProfileFailedEvent::from);
    }
}

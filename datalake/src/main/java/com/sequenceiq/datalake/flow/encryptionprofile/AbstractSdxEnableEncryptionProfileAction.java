package com.sequenceiq.datalake.flow.encryptionprofile;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileFailedEvent;

abstract class AbstractSdxEnableEncryptionProfileAction<P extends SdxEvent> extends AbstractSdxEncryptionProfileAction<P> {

    protected AbstractSdxEnableEncryptionProfileAction(Class<P> payloadClass) {
        super(payloadClass, SdxEnableEncryptionProfileFailedEvent::from);
    }
}

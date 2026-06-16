package com.sequenceiq.datalake.flow.trustedrealm.action;

import java.util.Optional;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmFailureResponse;
import com.sequenceiq.datalake.service.AbstractSdxAction;

public abstract class UpdateTrustedRealmAction<T extends SdxEvent> extends AbstractSdxAction<T> {

    protected UpdateTrustedRealmAction(Class<T> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected Object getFailurePayload(T payload, Optional<SdxContext> flowContext, Exception ex) {
        return new UpdateTrustedRealmFailureResponse(payload.getResourceId(), payload.getUserId(), ex);
    }
}

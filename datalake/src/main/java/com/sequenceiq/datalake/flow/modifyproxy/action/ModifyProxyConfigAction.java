package com.sequenceiq.datalake.flow.modifyproxy.action;

import java.util.Optional;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigFailureResponse;
import com.sequenceiq.datalake.service.AbstractSdxAction;

public abstract class ModifyProxyConfigAction<T extends SdxEvent> extends AbstractSdxAction<T> {

    protected ModifyProxyConfigAction(Class<T> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected Object getFailurePayload(T payload, Optional<SdxContext> flowContext, Exception ex) {
        return new ModifyProxyConfigFailureResponse(payload.getResourceId(), payload.getUserId(), ex);
    }
}

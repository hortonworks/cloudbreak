package com.sequenceiq.freeipa.flow.freeipa.trust.finish.action;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupAddTrustRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("FinishTrustSetupAddTrustAction")
public class FinishTrustSetupAddTrustAction extends AbstractFinishTrustSetupAction<FinishTrustSetupEvent> {

    public FinishTrustSetupAddTrustAction() {
        super(FinishTrustSetupEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, FinishTrustSetupEvent payload, Map<Object, Object> variables) throws Exception {
        setOperationId(variables, payload.getOperationId());
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.TRUST_SETUP_FINISH_IN_PROGRESS, "Add cross-realm trust to FreeIPA");
        FinishTrustSetupAddTrustRequest request = new FinishTrustSetupAddTrustRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FinishTrustSetupEvent payload, Optional<StackContext> flowContext, Exception ex) {
        return new FinishTrustSetupAddTrustFailed(payload.getResourceId(), ex);
    }
}

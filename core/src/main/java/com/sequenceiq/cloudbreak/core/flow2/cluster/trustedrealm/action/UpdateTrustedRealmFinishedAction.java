package com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.AbstractUpdateTrustedRealmAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmStatusService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmResult;

@Component("UPDATE_TRUSTED_REALM_FINISHED_STATE")
public class UpdateTrustedRealmFinishedAction extends AbstractUpdateTrustedRealmAction<UpdateTrustedRealmResult> {

    @Inject
    private UpdateTrustedRealmStatusService statusService;

    public UpdateTrustedRealmFinishedAction() {
        super(UpdateTrustedRealmResult.class);
    }

    @Override
    protected void doExecute(UpdateTrustedRealmContext context, UpdateTrustedRealmResult payload, Map<Object, Object> variables) {
        statusService.success(context.getStack().getId());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(UpdateTrustedRealmContext context) {
        return new StackEvent(UpdateTrustedRealmEvent.UPDATE_TRUSTED_REALM_FINALIZED_EVENT.event(), context.getStack().getId());
    }
}


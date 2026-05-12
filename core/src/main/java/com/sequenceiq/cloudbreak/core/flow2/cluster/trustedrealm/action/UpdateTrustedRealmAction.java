package com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.AbstractUpdateTrustedRealmAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm.UpdateTrustedRealmStatusService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.trustedrealm.UpdateTrustedRealmTriggerEvent;

@Component("UPDATE_TRUSTED_REALM_STATE")
public class UpdateTrustedRealmAction extends AbstractUpdateTrustedRealmAction<UpdateTrustedRealmTriggerEvent> {

    @Inject
    private UpdateTrustedRealmStatusService statusService;

    public UpdateTrustedRealmAction() {
        super(UpdateTrustedRealmTriggerEvent.class);
    }

    @Override
    protected void prepareExecution(UpdateTrustedRealmTriggerEvent payload, Map<Object, Object> variables) {
        variables.put(ENVIRONMENT_CRN, payload.getEnvironmentCrn());
        variables.put(REALM, payload.getRealm());
        variables.put(REMOVE, payload.isRemove());
    }

    @Override
    protected void doExecute(UpdateTrustedRealmContext context, UpdateTrustedRealmTriggerEvent payload, Map<Object, Object> variables) {
        statusService.updatingTrustedRealm(context.getStack().getId(), context.isRemove());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(UpdateTrustedRealmContext context) {
        return new UpdateTrustedRealmRequest(context.getStack().getId(), context.getEnvironmentCrn(), context.getRealm(), context.isRemove());
    }
}

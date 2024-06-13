package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.config.KerberosConfigUpdateService;

@Component("RebuildUpdateKerberosNameServersConfigAction")
public class RebuildUpdateKerberosNameServersConfigAction extends AbstractRebuildAction<StackEvent> {
    @Inject
    private KerberosConfigUpdateService kerberosConfigUpdateService;

    protected RebuildUpdateKerberosNameServersConfigAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Updating kerberos nameserver config");
        kerberosConfigUpdateService.updateNameservers(payload.getResourceId());
        sendEvent(context, new StackEvent(UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}

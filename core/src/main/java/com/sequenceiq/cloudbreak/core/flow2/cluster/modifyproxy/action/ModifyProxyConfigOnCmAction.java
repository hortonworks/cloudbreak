package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigContext;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigOnCmRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigRequest;

@Component("ModifyProxyConfigOnCmAction")
public class ModifyProxyConfigOnCmAction extends ModifyProxyConfigAction<ModifyProxyConfigRequest> {

    protected ModifyProxyConfigOnCmAction() {
        super(ModifyProxyConfigRequest.class);
    }

    @Override
    protected void doExecute(ModifyProxyConfigContext context, ModifyProxyConfigRequest payload, Map<Object, Object> variables) throws Exception {
        modifyProxyConfigStatusService().updateClusterManager(payload.getResourceId());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(ModifyProxyConfigContext context) {
        return new ModifyProxyConfigOnCmRequest(context.getStack().getId(), context.getPreviousProxyConfigCrn());
    }
}

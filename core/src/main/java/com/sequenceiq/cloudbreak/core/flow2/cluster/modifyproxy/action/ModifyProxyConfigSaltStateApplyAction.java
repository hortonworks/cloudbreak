package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigContext;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigSaltStateApplyRequest;

@Component("ModifyProxyConfigSaltStateApplyAction")
public class ModifyProxyConfigSaltStateApplyAction extends ModifyProxyConfigAction<ModifyProxyConfigRequest> {

    protected ModifyProxyConfigSaltStateApplyAction() {
        super(ModifyProxyConfigRequest.class);
    }

    @Override
    protected void prepareExecution(ModifyProxyConfigRequest payload, Map<Object, Object> variables) {
        super.prepareExecution(payload, variables);
        if (payload.getPreviousProxyConfigCrn() != null) {
            variables.put(PREVIOUS_PROXY_CONFIG, payload.getPreviousProxyConfigCrn());
        }
    }

    @Override
    protected void doExecute(ModifyProxyConfigContext context, ModifyProxyConfigRequest payload, Map<Object, Object> variables) throws Exception {
        modifyProxyConfigStatusService().applyingSaltState(payload.getResourceId());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(ModifyProxyConfigContext context) {
        return new ModifyProxyConfigSaltStateApplyRequest(context.getStack().getId(), context.getPreviousProxyConfigCrn());
    }
}

package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

@Component("ModifyProxyConfigFailedAction")
public class ModifyProxyConfigFailedAction extends ModifyProxyConfigAction<StackFailureEvent> {

    protected ModifyProxyConfigFailedAction() {
        super(StackFailureEvent.class);
    }

    @Override
    protected void doExecute(ModifyProxyConfigContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
        modifyProxyConfigStatusService().failed(payload.getResourceId(), payload.getException());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(ModifyProxyConfigContext context) {
        return new StackEvent(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_FAIL_HANDLED_EVENT.selector(), context.getStack().getId());
    }
}

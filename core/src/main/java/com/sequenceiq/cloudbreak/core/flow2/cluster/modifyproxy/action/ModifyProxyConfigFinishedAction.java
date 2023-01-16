package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@Component("ModifyProxyConfigFinishedAction")
public class ModifyProxyConfigFinishedAction extends ModifyProxyConfigAction<StackEvent> {

    protected ModifyProxyConfigFinishedAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(ModifyProxyConfigContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        modifyProxyConfigStatusService().success(payload.getResourceId());
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(ModifyProxyConfigContext context) {
        return new StackEvent(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_FINISHED_EVENT.selector(), context.getStack().getId());
    }
}

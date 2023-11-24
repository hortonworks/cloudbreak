package com.sequenceiq.freeipa.flow.stack.modify.proxy.action;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.ModifyProxyConfigContext;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.event.ModifyProxyConfigSaltStateApplyRequest;
import com.sequenceiq.freeipa.flow.stack.modify.proxy.event.ModifyProxyConfigTriggerEvent;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Component("ModifyProxyConfigSaltStateApplyAction")
public class ModifyProxyConfigSaltStateApplyAction extends ModifyProxyConfigAction<ModifyProxyConfigTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigSaltStateApplyAction.class);

    @Inject
    private StackUpdater stackUpdater;

    public ModifyProxyConfigSaltStateApplyAction() {
        super(ModifyProxyConfigTriggerEvent.class);
    }

    @Override
    protected void doExecute(ModifyProxyConfigContext context, ModifyProxyConfigTriggerEvent payload, Map<Object, Object> variables) throws Exception {
        LOGGER.info("Start modify proxy config salt state apply");
        setChainedAction(variables, payload.isChained());
        setFinalChain(variables, payload.isFinalChain());
        setOperationId(variables, payload.getOperationId());
        stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.MODIFY_PROXY_CONFIG_IN_PROGRESS,
                "Applying modified proxy config salt state");
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(ModifyProxyConfigContext context) {
        return new ModifyProxyConfigSaltStateApplyRequest(context.getStack().getId());
    }
}

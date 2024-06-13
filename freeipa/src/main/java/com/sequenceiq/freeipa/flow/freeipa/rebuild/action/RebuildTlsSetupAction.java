package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.TLS_SETUP_FINISHED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.action.StackProvisionService;

@Component("RebuildTlsSetupAction")
public class RebuildTlsSetupAction extends AbstractRebuildAction<StackEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildTlsSetupAction.class);

    @Inject
    private StackProvisionService provisionService;

    protected RebuildTlsSetupAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.REBUILD_IN_PROGRESS, "Setting up TLS");
        LOGGER.debug("Tunnel type is: {}", context.getStack().getTunnel());
        if (!context.getStack().getTunnel().useCcm()) {
            provisionService.setupTls(context);
        }
        sendEvent(context, new StackEvent(TLS_SETUP_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}

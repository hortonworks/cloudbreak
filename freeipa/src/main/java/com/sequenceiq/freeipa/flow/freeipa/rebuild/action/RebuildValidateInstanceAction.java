package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent.VALIDATE_INSTANCE_FINISHED_EVENT;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.freeipa.upscale.action.FreeIpaUpscaleActions;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

/**
 * TODO
 * Validate newly created instance on cloud is OK
 *
 * @see FreeIpaUpscaleActions#validateInstancesAction()
 */
@Component("RebuildValidateInstanceAction")
public class RebuildValidateInstanceAction extends AbstractRebuildAction<UpscaleStackResult> {
    protected RebuildValidateInstanceAction() {
        super(UpscaleStackResult.class);
    }

    @Override
    protected void doExecute(StackContext context, UpscaleStackResult payload, Map<Object, Object> variables) throws Exception {
        sendEvent(context, new StackEvent(VALIDATE_INSTANCE_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
